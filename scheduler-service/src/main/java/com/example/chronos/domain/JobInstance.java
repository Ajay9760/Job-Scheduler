package com.example.chronos.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_instances", indexes = {
        @Index(name = "idx_job_instances_job_id", columnList = "job_id"),
        @Index(name = "idx_job_instances_status", columnList = "status"),
        @Index(name = "idx_job_instances_scheduled_time", columnList = "scheduled_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstanceStatus status;

    @Column(name = "scheduled_time", nullable = false)
    private LocalDateTime scheduledTime;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "http_status_code")
    private Integer httpStatusCode;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = InstanceStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Integer getMaxRetries() {
        return job.getMaxRetries();
    }

    public enum InstanceStatus {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED,
        TIMEOUT,
        CANCELLED,
        RETRY_PENDING, SKIPPED
    }

    // Calculates duration if both start and completion times exist
    public void calculateDuration() {
        if (startedAt != null && completedAt != null) {
            this.durationMs = java.time.Duration.between(startedAt, completedAt).toMillis();
        }
    }

    // Marks as started
    public void markAsStarted() {
        this.status = InstanceStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Marks as completed with status
    public void markAsCompleted(InstanceStatus finalStatus, String message) {
        this.status = finalStatus;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (finalStatus == InstanceStatus.FAILED || finalStatus == InstanceStatus.TIMEOUT) {
            this.errorMessage = message;
        }
        calculateDuration();
    }
}