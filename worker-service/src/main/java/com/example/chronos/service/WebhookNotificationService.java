package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class WebhookNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotificationService.class);

    private final WebClient webClient;

    public WebhookNotificationService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public void notify(Job job, boolean success, String resultBody) {
        // No webhook configured, nothing to do
        if (job.getWebhookUrl() == null || job.getWebhookUrl().isBlank()) {
            return;
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("jobId", job.getId());
            payload.put("externalId", job.getExternalId());
            payload.put("status", success ? JobStatus.COMPLETED.name() : JobStatus.FAILED.name());
            payload.put("result", resultBody);
            payload.put("timestamp", Instant.now().toString());

            webClient
                    .post()
                    .uri(job.getWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)   // ✅ this is what the test expects
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .doOnError(ex ->
                            log.warn("Failed to send webhook for job {}: {}", job.getId(), ex.getMessage())
                    )
                    .subscribe();

        } catch (Exception e) {
            log.warn("Unexpected error while sending webhook for job {}: {}", job.getId(), e.getMessage());
        }
    }
}
