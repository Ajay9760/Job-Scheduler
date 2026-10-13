package com.example.chronos.worker;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.JobInstance.InstanceStatus;
import com.example.chronos.support.UrlSafetyValidator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Best-effort completion webhook: SSRF-checked, short timeout, never affects the job outcome. */
@Component
@Slf4j
public class WebhookNotifier {

    private final UrlSafetyValidator validator;
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(5)).build();

    public WebhookNotifier(UrlSafetyValidator validator) {
        this.validator = validator;
    }

    public void notify(Job job, Long instanceId, InstanceStatus status, Integer httpStatus, String error) {
        String url = job.getWebhookUrl();
        if (url == null || url.isBlank()) {
            return;
        }
        try {
            validator.validate(url);
            String json = "{\"jobId\":" + job.getId() + ",\"instanceId\":" + instanceId
                    + ",\"status\":\"" + status + "\",\"httpStatus\":" + httpStatus
                    + ",\"error\":" + quote(error) + "}";
            client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warn("Webhook for job {} failed: {}", job.getId(), e.toString());
        }
    }

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ") + "\"";
    }
}
