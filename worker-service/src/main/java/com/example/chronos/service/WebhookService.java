package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.JobInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final RestTemplate restTemplate;

    public void sendSuccessWebhook(Job job, JobInstance instance) {
        if (job.getWebhookUrl() == null) return;
        send(job.getWebhookUrl(), buildPayload("SUCCESS", job, instance, null));
    }

    public void sendFailureWebhook(Job job, JobInstance instance, String errorMessage) {
        if (job.getWebhookUrl() == null) return;
        send(job.getWebhookUrl(), buildPayload("FAILED", job, instance, errorMessage));
    }

    private Map<String, Object> buildPayload(String status, Job job, JobInstance instance, String error) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("jobId", job.getId());
        payload.put("jobName", job.getName());
        payload.put("instanceId", instance.getId());
        payload.put("status", status);
        payload.put("error", error);
        return payload;
    }

    private void send(String url, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(url, request, Void.class);
            log.info("Webhook sent to {}", url);
        } catch (Exception e) {
            log.error("Failed to send webhook to {}: {}", url, e.getMessage());
        }
    }
}
