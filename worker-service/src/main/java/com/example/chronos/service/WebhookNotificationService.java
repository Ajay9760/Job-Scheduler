package com.example.chronos.service;

import com.example.chronos.domain.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;

@Service
public class WebhookNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotificationService.class);

    private final WebClient webClient;

    public WebhookNotificationService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public void notify(Job job, boolean success, String result) {
        if (job.getWebhookUrl() == null || job.getWebhookUrl().isBlank()) return;
        try {
            Map<String, Object> body = Map.of(
                    "jobId", job.getId(),
                    "externalId", job.getExternalId(),
                    "status", success ? "COMPLETED" : "FAILED",
                    "timestamp", Instant.now().toString(),
                    "result", result
            );
            webClient.post()
                    .uri(job.getWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.error("Webhook failed for job {}: {}", job.getId(), e.getMessage());
        }
    }
}
