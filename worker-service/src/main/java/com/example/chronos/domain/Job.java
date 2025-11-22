package com.example.chronos.domain;

import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.domain.enums.JobStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String externalId = UUID.randomUUID().toString();

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 1000)
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private HttpMethodType httpMethod;

    @Column(length = 4000)
    private String requestBody;

    @Column(length = 1000)
    private String headersJson;

    @Column(length = 255)
    private String cronExpression;

    private Instant nextRunAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.PENDING;

    private int priority = 5;
    private int timeoutSeconds = 30;
    private int maxRetries = 3;
    private int retryCount = 0;
    private long backoffSeconds = 30;
    private String webhookUrl;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private String createdBy;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // getters and setters omitted for brevity, generate all
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

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

    public String getHeadersJson() { return headersJson; }
    public void setHeadersJson(String headersJson) { this.headersJson = headersJson; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public Instant getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(Instant nextRunAt) { this.nextRunAt = nextRunAt; }

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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
