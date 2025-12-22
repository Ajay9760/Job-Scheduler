package com.example.chronos.dto.job;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCreateRequest {

    @NotBlank(message = "Job name is required")
    private String name;

    private String description;

    @NotBlank(message = "Target URL is required")
    private String httpUrl; // Changed from targetUrl to httpUrl

    @NotBlank(message = "HTTP method is required")
    private String httpMethod;

    private Map<String, String> httpHeaders;

    private String httpBody; // Changed from requestBody to httpBody

    @NotBlank(message = "Schedule type is required")
    private String scheduleType;

    @NotBlank(message = "CRON expression is required")
    private String cronExpression;

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 10, message = "Priority must be at most 10")
    private Integer priority;

    @Min(value = 1, message = "Timeout must be at least 1 second")
    private Integer timeoutSeconds;

    @Min(value = 0, message = "Max retries cannot be negative")
    private Integer maxRetries;

    @Min(value = 1, message = "Backoff strategy must be at least 1 second")
    private String backoffStrategy;
    private String webhookUrl;

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
        this.backoffStrategy = backoffSeconds != null && backoffSeconds > 5 ? "LINEAR" : "EXPONENTIAL";
    }
}
