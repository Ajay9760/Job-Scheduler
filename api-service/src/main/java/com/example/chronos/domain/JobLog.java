package com.example.chronos.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "job_logs", indexes = {
        @Index(name = "idx_job_logs_instance_id", columnList = "instance_id"),
        @Index(name = "idx_job_logs_job_id", columnList = "job_id"),
        @Index(name = "idx_job_logs_level", columnList = "log_level"),
        @Index(name = "idx_job_logs_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    private JobInstance instance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", nullable = false)
    private LogLevel logLevel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (logLevel == null) {
            logLevel = LogLevel.INFO;
        }
    }

    public enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    // Static factory methods for convenience
    public static JobLog debug(JobInstance instance, String message) {
        return JobLog.builder()
                .instance(instance)
                .job(instance.getJob())
                .logLevel(LogLevel.DEBUG)
                .message(message)
                .build();
    }

    public static JobLog info(JobInstance instance, String message) {
        return JobLog.builder()
                .instance(instance)
                .job(instance.getJob())
                .logLevel(LogLevel.INFO)
                .message(message)
                .build();
    }

    public static JobLog warn(JobInstance instance, String message) {
        return JobLog.builder()
                .instance(instance)
                .job(instance.getJob())
                .logLevel(LogLevel.WARN)
                .message(message)
                .build();
    }

    public static JobLog error(JobInstance instance, String message) {
        return JobLog.builder()
                .instance(instance)
                .job(instance.getJob())
                .logLevel(LogLevel.ERROR)
                .message(message)
                .build();
    }

    public static JobLog withDetails(JobInstance instance, LogLevel level, String message, Map<String, Object> details) {
        return JobLog.builder()
                .instance(instance)
                .job(instance.getJob())
                .logLevel(level)
                .message(message)
                .details(details)
                .build();
    }
}
