package com.example.chronos.domain;

import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.domain.enums.JobStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_jobs_status", columnList = "status"),
        @Index(name = "idx_jobs_created_by", columnList = "createdBy"),
        @Index(name = "idx_jobs_next_run", columnList = "nextRunAt")
})
@Data
@NoArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HttpMethodType httpMethod;

    @Column(length = 4000)
    private String requestBody;

    @Column(nullable = false)
    private int priority = 5;

    @Column(nullable = false)
    private int timeoutSeconds = 30;

    @Column(nullable = false)
    private int maxRetries = 3;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column(nullable = false)
    private long backoffSeconds = 30;

    @Column(length = 1000)
    private String webhookUrl;

    @Column(nullable = false)
    private String cronExpression;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.SCHEDULED;

    @Column(nullable = false)
    private String createdBy; // This is your "owner" field

    @Column
    private Instant nextRunAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(length = 2000)
    private String lastError;

    @Column(name = "total_runs")
    private Integer totalRuns = 0;

    @Column(name = "successful_runs")
    private Integer successfulRuns = 0;

    @Column(name = "failed_runs")
    private Integer failedRuns = 0;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "last_failure_at")
    private Instant lastFailureAt;

    @Column(name = "avg_duration_ms")
    private Long avgDurationMs = 0L;

    @Column(name = "schedule_type", nullable = false, length = 20)
    private String scheduleType = "CRON";

    @Column(name = "consecutive_failures")
    private Integer consecutiveFailures = 0;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (externalId == null) {
            externalId = java.util.UUID.randomUUID().toString();
        }
        if (totalRuns == null) totalRuns = 0;
        if (successfulRuns == null) successfulRuns = 0;
        if (failedRuns == null) failedRuns = 0;
        if (avgDurationMs == null) avgDurationMs = 0L;
        if (consecutiveFailures == null) consecutiveFailures = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

}