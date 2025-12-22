package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.JobInstance;
import com.example.chronos.repository.JobInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;  // ✅ ADD THIS
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobConsumer {

    private final JobInstanceRepository instanceRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @RabbitListener(queues = "chronos.job.queue")
    @Transactional
    public void processJob(Map<String, Object> message) {
        Long instanceId = ((Number) message.get("instanceId")).longValue();
        log.info(" Processing job instance: {}", instanceId);

        JobInstance instance = instanceRepository.findById(instanceId).orElse(null);
        if (instance == null) {
            log.error(" Instance not found: {}", instanceId);
            return;
        }

        Job job = instance.getJob();

        try {
            if (job.getTargetUrl() == null || job.getTargetUrl().isEmpty()) {
                throw new IllegalStateException("Job targetUrl is null or empty");
            }
            if (job.getHttpMethod() == null) {
                throw new IllegalStateException("Job httpMethod is null");
            }

            // Update status to RUNNING
            instance.setStatus(JobInstance.InstanceStatus.RUNNING);
            instance.setStartedAt(LocalDateTime.now());
            instanceRepository.save(instance);

            log.info(" Instance {} started - calling {} {}",
                    instanceId, job.getHttpMethod(), job.getTargetUrl());

            // Execute HTTP request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String body = job.getRequestBody() != null ? job.getRequestBody() : "";
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            long startTime = System.currentTimeMillis();

            ResponseEntity<String> response = restTemplate.exchange(
                    job.getTargetUrl(),
                    HttpMethod.valueOf(job.getHttpMethod().name()),
                    entity,
                    String.class
            );

            long duration = System.currentTimeMillis() - startTime;

            // Update success
            instance.setStatus(JobInstance.InstanceStatus.SUCCESS);
            instance.setCompletedAt(LocalDateTime.now());
            instance.setDurationMs(duration);
            instance.setHttpStatusCode(response.getStatusCode().value());
            instance.setResponseBody(response.getBody());

            log.info("Job instance {} completed successfully in {}ms with status {}",
                    instanceId, duration, response.getStatusCode().value());

        } catch (Exception e) {
            log.error("Job instance {} failed: {}", instanceId, e.getMessage(), e);

            instance.setStatus(JobInstance.InstanceStatus.FAILED);
            instance.setCompletedAt(LocalDateTime.now());

            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 1000) {
                errorMsg = errorMsg.substring(0, 1000) + "...";
            }
            instance.setErrorMessage(errorMsg);
        } finally {
            instanceRepository.save(instance);
            log.info("Instance {} final status: {}", instanceId, instance.getStatus());
        }
    }
}