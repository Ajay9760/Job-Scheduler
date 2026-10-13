package com.example.chronos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * One execution attempt of a {@link Job}.
 *
 * <p>The row doubles as the outbox: the scheduler inserts it as {@code PENDING}, the dispatcher
 * publishes it to the broker and flips it to {@code QUEUED}, and a worker claims it with a
 * conditional update ({@code PENDING|QUEUED -> RUNNING}). Duplicate broker deliveries therefore
 * cannot cause a double execution.
 *
 * <p>Retries are new rows with {@code retryCount + 1} and a later {@code scheduledTime}, so the delay
 * lives in the database and survives restarts (the old implementation kept it in an in-memory timer).
 */
@Entity
@Table(name = "job_instances")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobInstance {

    public enum InstanceStatus {
        PENDING,
        QUEUED,
        RUNNING,
        SUCCESS,
        FAILED,
        TIMEOUT,
        CANCELLED,
        SKIPPED;

        private static final Set<InstanceStatus> TERMINAL =
                EnumSet.of(SUCCESS, FAILED, TIMEOUT, CANCELLED, SKIPPED);

        public boolean isTerminal() {
            return TERMINAL.contains(this);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** LAZY on purpose: the old EAGER mapping loaded a Job for every instance row (N+1). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false, updatable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InstanceStatus status = InstanceStatus.PENDING;

    @Column(name = "scheduled_time", nullable = false)
    private Instant scheduledTime;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "worker_id", length = 120)
    private String workerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = InstanceStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
