package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.JobInstance;
import com.example.chronos.repository.JobInstanceRepository;
import com.example.chronos.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;


@Service
@RequiredArgsConstructor
@Slf4j
public class JobExecutor {

    private final JobInstanceRepository instanceRepository;
    private final JobRepository jobRepository;
    private final RestTemplate restTemplate;
    private final WebhookService webhookService;
    private final RetryManager retryManager;

    /**
     * Execute a job instance
     */
    @Transactional
    public void executeJob(Long instanceId) {
        JobInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));

        Job job = instance.getJob();

        log.info("Executing job instance {} for job {} ({})", instanceId, job.getId(), job.getName());
        createLog(instance, LogLevel.INFO, "Starting job execution");

        // Mark as running
        instance.markAsStarted();
        instanceRepository.save(instance);

        try {
            // Execute the HTTP request
            ResponseEntity<String> response = executeHttpRequest(instance, job);

            // Handle success
            handleSuccess(instance, job, response);

        } catch (Exception e) {
            // Handle failure
            handleFailure(instance, job, e);
        }
    }

    /**
     * Execute the actual HTTP request
     */
    private ResponseEntity<String> executeHttpRequest(JobInstance instance, Job job) throws Exception {
        createLog(instance, LogLevel.INFO, "Sending HTTP request to: " + job.getTargetUrl());

        // Build headers (for now we ignore headersJson to keep it simple)
        HttpHeaders headers = new HttpHeaders();

        // Build request
        HttpEntity<String> request = new HttpEntity<>(job.getRequestBody(), headers);

        // Timeout is configured on RestTemplate bean; we just log the configured timeoutSeconds
        int timeoutSeconds = job.getTimeoutSeconds();

        createLog(instance, LogLevel.DEBUG,
                String.format("Request config - Method: %s, Timeout: %ds",
                        job.getHttpMethod(), timeoutSeconds));

        // Execute
        long startTime = System.currentTimeMillis();
        ResponseEntity<String> response;

        try {
            HttpMethod method = HttpMethod.valueOf(job.getHttpMethod().name());
            response = restTemplate.exchange(
                    job.getTargetUrl(),
                    method,
                    request,
                    String.class
            );
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            createLog(instance, LogLevel.ERROR,
                    "HTTP request failed after " + duration + "ms: " + e.getMessage());
            throw e;
        }

        long duration = System.currentTimeMillis() - startTime;
        createLog(instance, LogLevel.INFO,
                String.format("Received response - Status: %d, Duration: %dms",
                        response.getStatusCode().value(), duration));

        return response;
    }

    /**
     * Handle successful execution
     */
    @Transactional
    public void handleSuccess(JobInstance instance, Job job, ResponseEntity<String> response) {
        log.info("Job instance {} completed successfully", instance.getId());

        instance.setHttpStatusCode(response.getStatusCode().value());
        instance.setResponseBody(truncateResponse(response.getBody()));
        instance.markAsCompleted(JobInstance.InstanceStatus.SUCCESS, null);
        instanceRepository.save(instance);

        createLog(instance, LogLevel.INFO, "Job completed successfully");

        // Update job statistics (we only set lastError on failure, so nothing for success now)
        updateJobStatistics(job, true, instance.getDurationMs(), null);

        // Send webhook notification
        if (job.getWebhookUrl() != null) {
            webhookService.sendSuccessWebhook(job, instance);
        }
    }

    /**
     * Handle failed execution with retry logic
     */
    @Transactional
    public void handleFailure(JobInstance instance, Job job, Exception error) {
        log.error("Job instance {} failed: {}", instance.getId(), error.getMessage());

        String errorMessage = error.getMessage();
        instance.setErrorMessage(truncateError(errorMessage));

        createLog(instance, LogLevel.ERROR, "Job failed: " + errorMessage);

        // Check if retry is needed
        boolean shouldRetry = retryManager.shouldRetry(instance, job);

        if (shouldRetry) {
            int nextAttempt = (instance.getRetryCount() != null ? instance.getRetryCount() : 0) + 1;

            log.info("Scheduling retry for instance {} (attempt {}/{})",
                    instance.getId(), nextAttempt, job.getMaxRetries());

            instance.markAsCompleted(JobInstance.InstanceStatus.FAILED, errorMessage);
            instanceRepository.save(instance);

            createLog(instance, LogLevel.WARN,
                    String.format("Scheduling retry %d/%d", nextAttempt, job.getMaxRetries()));

            // Schedule retry
            retryManager.scheduleRetry(instance, job);
        } else {
            // Mark as permanently failed
            instance.markAsCompleted(JobInstance.InstanceStatus.FAILED, errorMessage);
            instanceRepository.save(instance);

            createLog(instance, LogLevel.ERROR, "Job failed permanently - no more retries");
        }

        // Update job statistics
        updateJobStatistics(job, false, instance.getDurationMs(), errorMessage);

        // Send webhook notification
        if (job.getWebhookUrl() != null) {
            webhookService.sendFailureWebhook(job, instance, errorMessage);
        }
    }

    /**
     * Update job statistics after execution.
     * For now we just set lastError on failure.
     */
    @Transactional
    public void updateJobStatistics(Job job, boolean success, Long durationMs, String errorMessage) {
        if (!success) {
            job.setLastError(errorMessage);
        }
        job.setUpdatedAt(java.time.Instant.now());
        jobRepository.save(job);
    }

    /**
     * Simple logger-based "log entry"
     */
    private void createLog(JobInstance instance, LogLevel level, String message) {
        String prefix = String.format("[instance=%d, job=%d] ",
                instance.getId(),
                instance.getJob() != null ? instance.getJob().getId() : null);

        switch (level) {
            case ERROR -> log.error(prefix + message);
            case WARN -> log.warn(prefix + message);
            case DEBUG -> log.debug(prefix + message);
            default -> log.info(prefix + message);
        }
    }

    /**
     * Truncate response body to avoid storing huge responses
     */
    private String truncateResponse(String response) {
        if (response == null) return null;
        int maxLength = 5000;
        return response.length() > maxLength
                ? response.substring(0, maxLength) + "... (truncated)"
                : response;
    }

    /**
     * Truncate error message
     */
    private String truncateError(String error) {
        if (error == null) return "Unknown error";
        int maxLength = 1000;
        return error.length() > maxLength
                ? error.substring(0, maxLength) + "... (truncated)"
                : error;
    }
}
