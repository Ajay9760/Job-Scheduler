package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.JobInstance;
import com.example.chronos.repository.JobInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobConsumer {

    private final JobInstanceRepository instanceRepository;
    private final RestTemplate restTemplate;

    @Transactional
    @RabbitListener(queues = "chronos.job.queue")
    public void processJob(Map<String, Object> message) {

        Long instanceId = ((Number) message.get("instanceId")).longValue();
        log.info("Processing job instance {}", instanceId);

        JobInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new IllegalStateException("Instance not found"));

        Job job = instance.getJob(); // SAFE (transaction open)

        instance.setStatus(JobInstance.InstanceStatus.RUNNING);
        instance.setStartedAt(LocalDateTime.now());
        instanceRepository.save(instance);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity =
                    new HttpEntity<>(job.getRequestBody(), headers);

            long start = System.currentTimeMillis();

            ResponseEntity<String> response = restTemplate.exchange(
                    job.getTargetUrl(),
                    HttpMethod.valueOf(job.getHttpMethod().name()),
                    entity,
                    String.class
            );

            long duration = System.currentTimeMillis() - start;

            // SUCCESS
            if (response.getStatusCode().is2xxSuccessful()) {
                instance.setStatus(JobInstance.InstanceStatus.SUCCESS);
                instance.setCompletedAt(LocalDateTime.now());
                instance.setDurationMs(duration);
                instance.setHttpStatusCode(response.getStatusCode().value());
                instance.setResponseBody(response.getBody());
                return;
            }

            // RETRYABLE
            if (isRetryable(response.getStatusCode())) {
                handleRetry(instance, response);
                return;
            }

            // NON-RETRYABLE
            fail(instance, response.getStatusCodeValue(),
                    "Non-retryable HTTP error");

        } catch (Exception e) {
            fail(instance, null, e.getMessage());
        }
    }

    private boolean isRetryable(HttpStatusCode statusCode) {
        return statusCode.value() == 500 ||
                statusCode.value() == 502 ||
                statusCode.value() == 503 ||
                statusCode.value() == 504;
    }

    private boolean isRetryable(HttpStatus status) {
        return status == HttpStatus.SERVICE_UNAVAILABLE ||
                status == HttpStatus.GATEWAY_TIMEOUT ||
                status == HttpStatus.BAD_GATEWAY ||
                status == HttpStatus.REQUEST_TIMEOUT;
    }

    private void handleRetry(JobInstance instance, ResponseEntity<String> response) {

        instance.setRetryCount(instance.getRetryCount() + 1);
        instance.setHttpStatusCode(response.getStatusCodeValue());

        if (instance.getRetryCount() <= instance.getMaxRetries()) {
            instance.setStatus(JobInstance.InstanceStatus.RETRY_PENDING);
            instance.setErrorMessage("Retryable failure: " +
                    response.getStatusCode());
            log.warn("Retrying instance {} (attempt {}/{})",
                    instance.getId(),
                    instance.getRetryCount(),
                    instance.getMaxRetries());
        } else {
            fail(instance, response.getStatusCodeValue(),
                    "Max retries exceeded");
        }
    }

    private void fail(JobInstance instance, Integer status, String message) {
        instance.setStatus(JobInstance.InstanceStatus.FAILED);
        instance.setCompletedAt(LocalDateTime.now());
        instance.setHttpStatusCode(status);
        instance.setErrorMessage(message);
    }
}
