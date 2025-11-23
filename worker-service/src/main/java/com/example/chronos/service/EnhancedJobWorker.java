package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
public class EnhancedJobWorker {

    private static final Logger log = LoggerFactory.getLogger(EnhancedJobWorker.class);

    private final JobRepository jobRepository;
    private final WebClient webClient;
    private final WebhookNotificationService webhookService;
    private final SimpleCircuitBreaker circuitBreaker;

    public EnhancedJobWorker(JobRepository jobRepository,
                             WebClient.Builder builder,
                             WebhookNotificationService webhookService,
                             SimpleCircuitBreaker circuitBreaker) {
        this.jobRepository = jobRepository;
        this.webClient = builder.build();
        this.webhookService = webhookService;
        this.circuitBreaker = circuitBreaker;
    }

    @RabbitListener(queues = "chronos.jobs.execute")
    public void handle(Long jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Job {} not found", jobId);
            return;
        }
        if (job.getStatus() == JobStatus.CANCELLED || job.getStatus() == JobStatus.PAUSED) {
            log.info("Job {} is cancelled / paused, skipping", jobId);
            return;
        }

        String key = job.getTargetUrl();
        if (circuitBreaker.isOpen(key)) {
            log.warn("Circuit open for {}, skipping job {}", key, jobId);
            return;
        }

        try (MDC.MDCCloseable ignored = MDC.putCloseable
                ("jobId", String.valueOf(jobId))) {
        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage());
        }

        try {
            execute(job);
            circuitBreaker.onSuccess(key);
        } catch (Exception e) {
            circuitBreaker.onFailure(key);
            log.error("Job {} failed: {}", jobId, e.getMessage());
        }
    }

    private void execute(Job job) {
        job.setStatus(JobStatus.RUNNING);
        jobRepository.save(job);

        HttpMethod method = HttpMethod.valueOf(job.getHttpMethod().name());
        int timeout = job.getTimeoutSeconds() > 0 ? job.getTimeoutSeconds() : 30;

        WebClient.RequestBodyUriSpec uriSpec = webClient.method(method);
        WebClient.RequestBodySpec bodySpec = uriSpec
                .uri(job.getTargetUrl())
                .contentType(MediaType.APPLICATION_JSON);

        WebClient.RequestHeadersSpec<?> requestSpec;
        if (job.getRequestBody() != null && !job.getRequestBody().isBlank()) {
            requestSpec = bodySpec.bodyValue(job.getRequestBody());
        } else {
            requestSpec = bodySpec;
        }

        String response = requestSpec
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeout))
                .block();

        job.setStatus(JobStatus.COMPLETED);
        job.setLastError(null);
        job.setRetryCount(0);
        job.setNextRunAt(null);
        jobRepository.save(job);

        webhookService.notify(job, true, response);
        log.info("Job {} completed", job.getId());
    }

}
