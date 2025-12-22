package com.example.chronos.domain;

import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.domain.enums.HttpMethodType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_jobs_status", columnList = "status"),
        @Index(name = "idx_jobs_next_run", columnList = "next_run_at"),
        @Index(name = "idx_jobs_external_id", columnList = "external_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", unique = true, nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(name = "target_url", nullable = false)
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false)
    private HttpMethodType httpMethod;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobStatus status = JobStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 5;

    @Column(name = "timeout_seconds")
    @Builder.Default
    private Integer timeoutSeconds = 30;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "backoff_seconds")
    @Builder.Default
    private Long backoffSeconds = 60L;

    @Column(name = "webhook_url")
    private String webhookUrl;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "total_runs")
    @Builder.Default
    private Integer totalRuns = 0;

    @Column(name = "successful_runs")
    @Builder.Default
    private Integer successfulRuns = 0;

    @Column(name = "failed_runs")
    @Builder.Default
    private Integer failedRuns = 0;

    @Column(name = "avg_duration_ms")
    @Builder.Default
    private Long avgDurationMs = 0L;

    @Column(name = "consecutive_failures")
    @Builder.Default
    private Integer consecutiveFailures = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = JobStatus.ACTIVE;
        }
        if (externalId == null) {
            externalId = java.util.UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}