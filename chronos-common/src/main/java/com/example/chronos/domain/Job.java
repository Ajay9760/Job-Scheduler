package com.example.chronos.domain;

import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.domain.enums.ScheduleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;
import java.util.Map;

/**
 * A scheduled job definition. This is the single entity shared by api-service,
 * scheduler-service and worker-service (previously each service had its own,
 * diverging copy mapped to the same table).
 *
 * All instants are UTC. The job's {@link #timezone} only matters when evaluating
 * CRON expressions.
 */
@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Optimistic lock: a concurrent API edit and scheduler update cannot silently overwrite each other. */
    @Version
    private Long version;

    @Column(name = "external_id", nullable = false, updatable = false, length = 64)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "target_url", nullable = false, length = 2000)
    private String targetUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false, length = 10)
    private HttpMethodType httpMethod = HttpMethodType.GET;

    @Column(name = "request_body", columnDefinition = "text")
    private String requestBody;

    /** Extra request headers. May contain credentials: never return values from the API. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_headers", columnDefinition = "jsonb")
    private Map<String, String> requestHeaders;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 20)
    private ScheduleType scheduleType = ScheduleType.CRON;

    /** Schedule expression for {@link #scheduleType} (cron / interval / ISO instant). */
    @Column(name = "cron_expression", nullable = false)
    private String cronExpression;

    @Column(nullable = false, length = 64)
    private String timezone = "UTC";

    @Column(nullable = false)
    private int priority = 5;

    @Column(name = "timeout_seconds", nullable = false)
    private int timeoutSeconds = 30;

    @Column(name = "max_retries", nullable = false)
    private int maxRetries = 3;

    @Column(name = "backoff_seconds", nullable = false)
    private long backoffSeconds = 30;

    @Column(name = "webhook_url", length = 2000)
    private String webhookUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.ACTIVE;

    @Column(name = "created_by", nullable = false, length = 100, updatable = false)
    private String createdBy;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "last_success_at")
    private Instant lastSuccessAt;

    @Column(name = "last_failure_at")
    private Instant lastFailureAt;

    @Column(name = "total_runs", nullable = false)
    private long totalRuns = 0;

    @Column(name = "successful_runs", nullable = false)
    private long successfulRuns = 0;

    @Column(name = "failed_runs", nullable = false)
    private long failedRuns = 0;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures = 0;

    @Column(name = "avg_duration_ms", nullable = false)
    private long avgDurationMs = 0;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (externalId == null) {
            externalId = UUID.randomUUID().toString();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
