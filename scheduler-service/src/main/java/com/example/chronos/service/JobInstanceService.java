package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.JobInstance;
import com.example.chronos.repository.JobINstanceRepository;
import com.example.chronos.repository.JobRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobInstanceService {

    private final JobINstanceRepository instanceRepository;
    private final JobRepository jobRepository;
    private final RabbitTemplate rabbitTemplate;
    private final CronCalculator cronCalculator;

    /**
     * Creates a new JobInstance for a scheduled job and sends it to the worker queue
     */
    @Transactional
    public JobInstance createAndScheduleInstance(Job job) {
        log.info("Creating instance for job: {} (ID: {})", job.getName(), job.getId());

        // Create the instance
        JobInstance instance = JobInstance.builder()
                .job(job)
                .status(JobInstance.InstanceStatus.PENDING)
                .scheduledTime(LocalDateTime.now())
                .retryCount(0)
                .build();

        // Save to database
        instance = instanceRepository.save(instance);
        log.debug("Created instance ID: {} for job ID: {}", instance.getId(), job.getId());

        // Send to RabbitMQ for execution
        sendToWorkerQueue(instance);

        // Update job's next run time
        updateJobNextRun(job);

        return instance;
    }

    /**
     * Sends the job instance to RabbitMQ for worker processing
     */
    public void sendToWorkerQueue(JobInstance instance) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("instanceId", instance.getId());
            message.put("jobId", instance.getJob().getId());
            message.put("scheduledTime", instance.getScheduledTime().toString());

            rabbitTemplate.convertAndSend("chronos.exchange", "job.execute", message);
            log.info("Sent instance ID {} to worker queue", instance.getId());
        } catch (Exception e) {
            log.error("Failed to send instance {} to queue: {}", instance.getId(), e.getMessage());
            instance.setStatus(JobInstance.InstanceStatus.FAILED);
            instance.setErrorMessage("Failed to queue for execution: " + e.getMessage());
            instanceRepository.save(instance);
        }
    }

    /**
     * Updates job's next_run_at based on CRON schedule
     */
    /**
     * Updates job's next_run_at based on CRON schedule
     */
    @Transactional
    public void updateJobNextRun(Job job) {
        try {
            // Calculate the next run time based on current nextRunAt or now
            LocalDateTime baseTime = job.getNextRunAt() != null
                    ? LocalDateTime.ofInstant(job.getNextRunAt(), java.time.ZoneId.systemDefault())
                    : LocalDateTime.now();

            LocalDateTime nextRunLocal = cronCalculator.calculateNextRun(
                    job.getScheduleType(),
                    job.getCronExpression(),
                    baseTime
            );

            // ✅ Convert LocalDateTime to Instant and set next run
            if (nextRunLocal != null) {
                Instant nextRunInstant = nextRunLocal.atZone(java.time.ZoneId.systemDefault()).toInstant();
                job.setNextRunAt(nextRunInstant);
            }

            // ✅ Set last run time and increment total runs
            job.setLastRunAt(Instant.now());
            job.setTotalRuns(job.getTotalRuns() != null ? job.getTotalRuns() + 1 : 1);

            jobRepository.save(job);

            log.debug("Updated job {} next run to: {}", job.getId(), nextRunLocal);
        } catch (Exception e) {
            log.error("Failed to calculate next run for job {}: {}", job.getId(), e.getMessage());
        }
    }
    /**
     * Creates instance for manual trigger (runs immediately)
     */
    @Transactional
    public JobInstance createManualInstance(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        log.info("Creating manual instance for job: {}", job.getName());

        JobInstance instance = JobInstance.builder()
                .job(job)
                .status(JobInstance.InstanceStatus.PENDING)
                .scheduledTime(LocalDateTime.now())
                .retryCount(0)
                .build();

        instance = instanceRepository.save(instance);
        sendToWorkerQueue(instance);

        return instance;
    }

    /**
     * Creates retry instance for failed jobs
     */
    @Transactional
    public JobInstance createRetryInstance(JobInstance failedInstance) {
        log.info("Creating retry instance for failed instance: {}", failedInstance.getId());

        JobInstance retryInstance = JobInstance.builder()
                .job(failedInstance.getJob())
                .status(JobInstance.InstanceStatus.PENDING)
                .scheduledTime(LocalDateTime.now())
                .retryCount(failedInstance.getRetryCount() + 1)
                .build();

        retryInstance = instanceRepository.save(retryInstance);
        sendToWorkerQueue(retryInstance);

        return retryInstance;
    }

    /**
     * Marks stuck running instances as timeout
     */
    @Transactional
    public void cleanupStuckInstances() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(30);
        var stuckInstances = instanceRepository.findStuckRunningInstances(timeoutThreshold);

        for (JobInstance instance : stuckInstances) {
            log.warn("Marking stuck instance {} as TIMEOUT", instance.getId());
            instance.markAsCompleted(JobInstance.InstanceStatus.TIMEOUT, "Instance exceeded maximum execution time");
            instanceRepository.save(instance);
        }
    }
}