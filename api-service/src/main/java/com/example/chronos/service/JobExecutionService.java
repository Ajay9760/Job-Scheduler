package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.JobInstance;
import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.dto.job.JobInstanceDTO;
import com.example.chronos.repository.JobInstanceRepository;
import com.example.chronos.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobExecutionService {

    private final JobRepository jobRepository;
    private final JobInstanceRepository instanceRepository;

    /**
     * Trigger manual execution of a job
     */
    @Transactional
    public JobInstanceDTO triggerManualExecution(Long jobId) {
        log.info("🔥 Triggering manual execution for job ID: {}", jobId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        // ✅ FIX: Allow SCHEDULED jobs to be triggered
        if (job.getStatus() != JobStatus.SCHEDULED && job.getStatus() != JobStatus.ACTIVE) {
            throw new IllegalStateException("Cannot trigger inactive job: " + job.getName());
        }

        log.info("Manually triggering job: {} (ID: {})", job.getName(), jobId);

        // Create instance
        JobInstance instance = JobInstance.builder()
                .job(job)
                .status(JobInstance.InstanceStatus.PENDING)
                .scheduledTime(LocalDateTime.now())
                .retryCount(0)
                .build();

        instance = instanceRepository.save(instance);
        log.info("Created manual instance ID: {} for job: {}", instance.getId(), job.getName());

        return mapToDTO(instance);
    }

    /**
     * Resume a paused job
     */
    @Transactional
    public void resumeJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.getStatus() != JobStatus.PAUSED) {
            throw new IllegalStateException("Job is not paused: " + job.getName());
        }

        job.setStatus(JobStatus.SCHEDULED);
        jobRepository.save(job);
        log.info("Resumed job: {} (ID: {})", job.getName(), jobId);
    }

    /**
     * Stop a running instance
     */
    @Transactional
    public void stopInstance(Long instanceId) {
        JobInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        // ✅ FIX: Check if actually running
        if (instance.getStatus() != JobInstance.InstanceStatus.RUNNING) {
            throw new IllegalStateException("Instance is not running");
        }

        instance.markAsCompleted(JobInstance.InstanceStatus.CANCELLED, "Manually stopped");
        instanceRepository.save(instance);
        log.info("Stopped instance ID: {}", instanceId);
    }

    /**
     * Retry a failed instance
     */
    @Transactional
    public JobInstanceDTO retryFailedInstance(Long instanceId) {
        JobInstance failedInstance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        if (failedInstance.getStatus() != JobInstance.InstanceStatus.FAILED) {
            throw new IllegalStateException("Instance is not in FAILED state");
        }

        // Create retry instance
        JobInstance retryInstance = JobInstance.builder()
                .job(failedInstance.getJob())
                .status(JobInstance.InstanceStatus.PENDING)
                .scheduledTime(LocalDateTime.now())
                .retryCount(failedInstance.getRetryCount() + 1)
                .build();

        retryInstance = instanceRepository.save(retryInstance);
        log.info("Created retry instance ID: {} for failed instance: {}",
                retryInstance.getId(), instanceId);

        return mapToDTO(retryInstance);
    }

    /**
     * Enable a job
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
     * Disable a job
     */
    @Transactional
    public void disableJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        job.setStatus(JobStatus.PAUSED);
        jobRepository.save(job);
        log.info("Disabled job: {} (ID: {})", job.getName(), jobId);
    }

    /**
     * Bulk trigger jobs
     * ✅ FIX: Only trigger valid jobs and return accurate count
     */
    @Transactional
    public int bulkTrigger(List<Long> jobIds) {
        if (jobIds == null || jobIds.isEmpty()) {
            log.warn("Bulk trigger called with empty job IDs list");
            return 0;
        }

        int successCount = 0;
        log.info("Bulk triggering {} jobs", jobIds.size());

        for (Long jobId : jobIds) {
            try {
                Job job = jobRepository.findById(jobId).orElse(null);

                if (job == null) {
                    log.warn("Job not found: {}", jobId);
                    continue;
                }

                // ✅ FIX: Only trigger SCHEDULED/ACTIVE jobs
                if (job.getStatus() != JobStatus.SCHEDULED && job.getStatus() != JobStatus.ACTIVE) {
                    log.warn("Cannot trigger inactive job: {} (status: {})",
                            job.getName(), job.getStatus());
                    continue;
                }

                // Create instance
                JobInstance instance = JobInstance.builder()
                        .job(job)
                        .status(JobInstance.InstanceStatus.PENDING)
                        .scheduledTime(LocalDateTime.now())
                        .retryCount(0)
                        .build();

                instanceRepository.save(instance);
                successCount++;
                log.info("Successfully triggered job: {} (ID: {})", job.getName(), jobId);

            } catch (Exception e) {
                log.error("Failed to trigger job {}: {}", jobId, e.getMessage());
            }
        }

        log.info("Bulk trigger completed: {}/{} jobs triggered", successCount, jobIds.size());
        return successCount;
    }

    /**
     * Map JobInstance to DTO
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