package com.example.chronos.service;

import com.example.chronos.domain.JobInstance;
import com.example.chronos.domain.Job;
import com.example.chronos.repository.JobInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetryManager {

    private final JobInstanceRepository instanceRepository;
    private final RabbitTemplate rabbitTemplate;
    private final TaskScheduler taskScheduler;

    //  Determine if a failed job should be retried based on job.maxRetries

    public boolean shouldRetry(JobInstance instance, Job job) {
        int maxRetries = job.getMaxRetries();          // from Job entity
        int currentRetryCount = instance.getRetryCount() != null
                ? instance.getRetryCount()
                : 0;

        boolean shouldRetry = currentRetryCount < maxRetries;

        log.debug("Retry check for instance {}: attempt {}/{}, shouldRetry={}",
                instance.getId(), currentRetryCount, maxRetries, shouldRetry);

        return shouldRetry;
    }

    // Schedule a retry for a failed instance

    @Transactional
    public void scheduleRetry(JobInstance failedInstance, Job job) {
        int nextRetryCount = (failedInstance.getRetryCount() != null
                ? failedInstance.getRetryCount()
                : 0) + 1;

        long delaySeconds = calculateRetryDelay(nextRetryCount, job);

        log.info("Scheduling retry {} for instance {} with delay of {}s",
                nextRetryCount, failedInstance.getId(), delaySeconds);

        // Create new instance for retry
        JobInstance retryInstance = JobInstance.builder()
                .job(job)
                .status(JobInstance.InstanceStatus.PENDING)
                .scheduledTime(LocalDateTime.now().plusSeconds(delaySeconds))
                .retryCount(nextRetryCount)
                .build();

        retryInstance = instanceRepository.save(retryInstance);

        // Schedule the retry message to be sent after delay
        scheduleRetryMessage(retryInstance.getId(), delaySeconds);
    }

    // Calculate retry delay using exponential backoff based on job.backoffSeconds as base.

    private long calculateRetryDelay(int retryAttempt, Job job) {
        long base = job.getBackoffSeconds() > 0 ? job.getBackoffSeconds() : 30L;
        long delay = (long) (base * Math.pow(2, retryAttempt - 1));
        long maxDelay = 300L; // cap at 5 minutes
        return Math.min(delay, maxDelay);
    }

    // Schedule a delayed message to trigger retry

    private void scheduleRetryMessage(Long instanceId, long delaySeconds) {
        Instant executionTime = Instant.now().plusSeconds(delaySeconds);

        taskScheduler.schedule(() -> {
            try {
                sendRetryMessage(instanceId);
            } catch (Exception e) {
                log.error("Failed to send retry message for instance {}: {}", instanceId, e.getMessage());
            }
        }, executionTime);

        log.debug("Scheduled retry message for instance {} at {}", instanceId, executionTime);
    }

    //Send retry message to worker queue

    private void sendRetryMessage(Long instanceId) {
        Map<String, Object> message = new HashMap<>();
        message.put("instanceId", instanceId);
        message.put("isRetry", true);

        rabbitTemplate.convertAndSend("chronos.exchange", "job.execute", message);
        log.info("Sent retry message for instance {}", instanceId);
    }

    //Process retry instances that are ready to execute

    @Transactional
    public void processReadyRetries() {
        var readyRetries = instanceRepository.findPendingInstancesReadyToRun(LocalDateTime.now());

        log.info("Found {} retry instances ready to execute", readyRetries.size());

        for (JobInstance instance : readyRetries) {
            if (instance.getRetryCount() != null && instance.getRetryCount() > 0) {
                log.info("Processing retry instance {}: attempt {}",
                        instance.getId(), instance.getRetryCount());
                sendRetryMessage(instance.getId());
            }
        }
    }

    //Get retry statistics for a job

    public Map<String, Object> getRetryStats(Long jobId) {
        Map<String, Object> stats = new HashMap<>();

        var instances = instanceRepository.findByJobId(
                jobId, org.springframework.data.domain.Pageable.unpaged());

        long totalRetries = 0;
        long maxRetries = 0;

        for (JobInstance instance : instances) {
            if (instance.getRetryCount() != null && instance.getRetryCount() > 0) {
                totalRetries += instance.getRetryCount();
                maxRetries = Math.max(maxRetries, instance.getRetryCount());
            }
        }

        stats.put("totalRetries", totalRetries);
        stats.put("maxRetryAttempts", maxRetries);
        stats.put("instancesWithRetries", totalRetries > 0 ? 1 : 0);

        return stats;
    }

    // Cancel pending retries for a job

    @Transactional
    public int cancelPendingRetries(Long jobId) {
        var pendingRetries = instanceRepository.findByJobIdAndStatus(
                jobId,
                JobInstance.InstanceStatus.PENDING,
                org.springframework.data.domain.Pageable.unpaged()
        );

        int cancelledCount = 0;
        for (JobInstance instance : pendingRetries) {
            if (instance.getRetryCount() != null && instance.getRetryCount() > 0) {
                instance.setStatus(JobInstance.InstanceStatus.CANCELLED);
                instanceRepository.save(instance);
                cancelledCount++;
            }
        }

        log.info("Cancelled {} pending retries for job {}", cancelledCount, jobId);
        return cancelledCount;
    }
}
