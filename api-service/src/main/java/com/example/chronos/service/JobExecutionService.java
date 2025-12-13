package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.JobInstance;
import com.example.chronos.domain.enums.JobStatus;

import com.example.chronos.dto.job.JobInstanceDTO;
import com.example.chronos.repository.JobInstanceRepository;
import com.example.chronos.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobExecutionService {

    private final JobRepository jobRepository;
    private final JobInstanceRepository instanceRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public JobInstanceDTO triggerManualExecution(Long jobId) {
        log.info("🚀 Triggering manual execution for job ID: {}", jobId);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.SCHEDULED) { // Use enum
            throw new IllegalStateException("Cannot trigger inactive job: " + job.getName());
        }

        log.info("Manually triggering job: {} (ID: {})", job.getName(), jobId);

        // Create instance
        JobInstance instance = JobInstance.builder()
                .job(job)
                .status(JobInstance.InstanceStatus.PENDING)
                .scheduledTime(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()))
                .retryCount(0)
                .build();

        instance = instanceRepository.save(instance);

        // Send to queue
        sendToQueue(instance);

        return mapToDTO(instance);
    }

    /**
     * Pause a job (prevents future executions)
     */
    @Transactional
    public void pauseJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        job.setStatus(JobStatus.PAUSED); // Line 67
        jobRepository.save(job);

        log.info("Paused job: {} (ID: {})", job.getName(), jobId);
    }

    /**
     * Resume a paused job
     */
    @Transactional
    public void resumeJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.PAUSED) { // Use enum
            throw new IllegalStateException("Job is not paused: " + job.getName());
        }

        job.setStatus(JobStatus.SCHEDULED); // Line 85
        jobRepository.save(job);

        log.info("Resumed job: {} (ID: {})", job.getName(), jobId);
    }

    /**
     * Stop a currently running instance
     */
    @Transactional
    public void stopInstance(Long instanceId) {
        JobInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        if (instance.getStatus() != JobInstance.InstanceStatus.RUNNING) {
            throw new IllegalStateException("Instance is not running");
        }

        instance.setStatus(JobInstance.InstanceStatus.CANCELLED);
        instance.setCompletedAt(LocalDateTime.from(Instant.now()));
        instance.setErrorMessage("Manually cancelled by user");
        instanceRepository.save(instance);

        log.info("Stopped running instance: {}", instanceId);
    }

    /**
     * Retry a failed instance
     */
    @Transactional
    public JobInstanceDTO retryFailedInstance(Long instanceId) {
        JobInstance failedInstance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        if (failedInstance.getStatus() != JobInstance.InstanceStatus.FAILED) {
            throw new IllegalStateException("Instance is not in failed state");
        }

        log.info("Creating retry for failed instance: {}", instanceId);

        // Create new retry instance
        JobInstance retryInstance = JobInstance.builder()
                .job(failedInstance.getJob())
                .status(JobInstance.InstanceStatus.PENDING)
                .scheduledTime(LocalDateTime.from(Instant.now()))
                .retryCount((failedInstance.getRetryCount() != null ? failedInstance.getRetryCount() : 0) + 1)
                .build();

        retryInstance = instanceRepository.save(retryInstance);
        sendToQueue(retryInstance);

        return mapToDTO(retryInstance);
    }

    /**
     * Enable a disabled job
     */
    @Transactional
    public void enableJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        job.setStatus(JobStatus.SCHEDULED);
        jobRepository.save(job);

        log.info("Enabled job: {} (ID: {})", job.getName(), jobId);
    }

    /**
     * Disable a job (stops all future executions)
     */
    @Transactional
    public void disableJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        job.setStatus(JobStatus.COMPLETED);
        jobRepository.save(job);

        // Cancel any pending instances
        var pendingInstances = instanceRepository.findByJobIdAndStatus(
                jobId, JobInstance.InstanceStatus.PENDING, org.springframework.data.domain.Pageable.unpaged());

        for (JobInstance instance : pendingInstances) {
            instance.setStatus(JobInstance.InstanceStatus.CANCELLED);
            instance.setCompletedAt(LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
            instanceRepository.save(instance);
        }

        log.info("Disabled job and cancelled {} pending instances: {} (ID: {})",
                pendingInstances.getTotalElements(), job.getName(), jobId);
    }

    /**
     * Bulk trigger multiple jobs
     */
    @Transactional
    public int bulkTrigger(List<Long> jobIds) {
        int triggered = 0;

        for (Long jobId : jobIds) {
            try {
                triggerManualExecution(jobId);
                triggered++;
            } catch (Exception e) {
                log.error("Failed to trigger job {}: {}", jobId, e.getMessage());
            }
        }

        log.info("Bulk triggered {} out of {} jobs", triggered, jobIds.size());
        return triggered;
    }

    /**
     * Send instance to RabbitMQ queue
     */
    private void sendToQueue(JobInstance instance) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("instanceId", instance.getId());
            message.put("jobId", instance.getJob().getId());
            message.put("scheduledTime", instance.getScheduledTime().toString());

            rabbitTemplate.convertAndSend("chronos.exchange", "job.execute", message);
            log.debug("Sent instance {} to queue", instance.getId());
        } catch (Exception e) {
            log.error("Failed to send instance {} to queue: {}", instance.getId(), e.getMessage());
            instance.setStatus(JobInstance.InstanceStatus.FAILED);
            instance.setErrorMessage("Failed to queue: " + e.getMessage());
            instanceRepository.save(instance);
        }
    }

    /**
     * Map entity to DTO
     */
    private JobInstanceDTO mapToDTO(JobInstance instance) {
        JobInstanceDTO dto = JobInstanceDTO.builder()
                .id(instance.getId())
                .jobId(instance.getJob().getId())
                .jobName(instance.getJob().getName())
                .status(instance.getStatus())
                .scheduledTime(instance.getScheduledTime())
                .startedAt(instance.getStartedAt())
                .completedAt(instance.getCompletedAt())
                .durationMs(instance.getDurationMs())
                .httpStatusCode(instance.getHttpStatusCode())
                .responseBody(instance.getResponseBody())
                .errorMessage(instance.getErrorMessage())
                .retryCount(instance.getRetryCount())
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .build();

        dto.computeDerivedFields();
        return dto;
    }
}