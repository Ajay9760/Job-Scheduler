package com.example.chronos.dto.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class JobCreateRequest {

    @NotBlank(message = "Job name is required")
    private String name;

    private String description;

    @NotBlank(message = "Target URL is required")
    private String httpUrl; // Changed from targetUrl to httpUrl

    @NotBlank(message = "HTTP method is required")
    private String httpMethod;

    private Map<String, String> httpHeaders; // NEW FIELD

    private String httpBody; // Changed from requestBody to httpBody

    @NotBlank(message = "Schedule type is required")
    private String scheduleType; // NEW FIELD (CRON, INTERVAL, ONCE)

    @NotBlank(message = "CRON expression is required")
    private String cronExpression;

    private Integer priority;

    private Integer timeoutSeconds;

    private Integer maxRetries;

    private String backoffStrategy; // Changed from backoffSeconds to backoffStrategy

    private String webhookUrl;

    // For backward compatibility, keep old getters/setters
    public String getTargetUrl() {
        return httpUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.httpUrl = targetUrl;
    }

    public String getRequestBody() {
        return httpBody;
    }

    public void setRequestBody(String requestBody) {
        this.httpBody = requestBody;
    }

    public Integer getBackoffSeconds() {
        return backoffStrategy != null ?
                ("EXPONENTIAL".equals(backoffStrategy) ? 2 : 10) : null;
    }

    public void setBackoffSeconds(Integer backoffSeconds) {
        // Convert to strategy
        this.backoffStrategy = backoffSeconds != null && backoffSeconds > 5 ? "LINEAR" : "EXPONENTIAL";
    }
}