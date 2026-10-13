package com.example.chronos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Execution log line for a job instance. References are plain ids (no associations) so the worker can
 * write logs without loading entities and readers never hit lazy-loading problems.
 */
@Entity
@Table(name = "job_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobLog {

    public enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instance_id", nullable = false, updatable = false)
    private Long instanceId;

    @Column(name = "job_id", nullable = false, updatable = false)
    private Long jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", nullable = false, length = 10)
    @Builder.Default
    private LogLevel logLevel = LogLevel.INFO;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (logLevel == null) {
            logLevel = LogLevel.INFO;
        }
    }

    public static JobLog of(Long jobId, Long instanceId, LogLevel level, String message, Map<String, Object> details) {
        return JobLog.builder()
                .jobId(jobId)
                .instanceId(instanceId)
                .logLevel(level)
                .message(message)
                .details(details)
                .build();
    }
}
