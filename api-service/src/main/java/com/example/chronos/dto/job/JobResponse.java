package com.example.chronos.dto.job;

import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.domain.enums.JobStatus;

import java.time.Instant;

public class JobResponse {

    private Long id;
    private String externalId;
    private String name;
    private String targetUrl;
    private HttpMethodType httpMethod;
    private String cronExpression;
    private JobStatus status;
    private int priority;
    private int timeoutSeconds;
    private int maxRetries;
    private int retryCount;
    private long backoffSeconds;
    private String webhookUrl;
    private Instant nextRunAt;
    private Instant createdAt;
    private Instant updatedAt;
    private String lastError;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }

    public HttpMethodType getHttpMethod() { return httpMethod; }
    public void setHttpMethod(HttpMethodType httpMethod) { this.httpMethod = httpMethod; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public long getBackoffSeconds() { return backoffSeconds; }
    public void setBackoffSeconds(long backoffSeconds) { this.backoffSeconds = backoffSeconds; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public Instant getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(Instant nextRunAt) { this.nextRunAt = nextRunAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
