package com.example.chronos.api.dto;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.JobInstance;
import com.example.chronos.domain.JobLog;
import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.domain.enums.ScheduleType;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Map;

/** Request/response shapes. Entities are never exposed directly. Timestamps are UTC ISO-8601. */
public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) @Pattern(regexp = "[A-Za-z0-9._-]+") String username,
            @NotBlank @Size(min = 10, max = 100) String password) {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record TokenResponse(String token) {}

    /** Used for create (all required fields present) and update (unset fields keep their value). */
    public record JobRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 1000) String description,
            @NotBlank @Size(max = 2000) String targetUrl,
            @NotNull HttpMethodType httpMethod,
            String requestBody,
            Map<String, String> requestHeaders,
            ScheduleType scheduleType,
            @NotBlank @Size(max = 255) String cronExpression,
            @Size(max = 64) String timezone,
            @Min(1) @Max(10) Integer priority,
            @Min(1) @Max(300) Integer timeoutSeconds,
            @Min(0) @Max(10) Integer maxRetries,
            @Min(1) @Max(3600) Long backoffSeconds,
            @Size(max = 2000) String webhookUrl) {}

    public record JobResponse(
            Long id, String externalId, String name, String description, String targetUrl, String httpMethod,
            String scheduleType, String cronExpression, String timezone, int priority, int timeoutSeconds,
            int maxRetries, long backoffSeconds, String webhookUrl, String status, Instant nextRunAt,
            Instant lastRunAt, Instant lastSuccessAt, Instant lastFailureAt, long totalRuns, long successfulRuns,
            long failedRuns, int consecutiveFailures, String lastError, Instant createdAt) {
        public static JobResponse of(Job j) {
            return new JobResponse(j.getId(), j.getExternalId(), j.getName(), j.getDescription(), j.getTargetUrl(),
                    j.getHttpMethod().name(), j.getScheduleType().name(), j.getCronExpression(), j.getTimezone(),
                    j.getPriority(), j.getTimeoutSeconds(), j.getMaxRetries(), j.getBackoffSeconds(), j.getWebhookUrl(),
                    j.getStatus().name(), j.getNextRunAt(), j.getLastRunAt(), j.getLastSuccessAt(), j.getLastFailureAt(),
                    j.getTotalRuns(), j.getSuccessfulRuns(), j.getFailedRuns(), j.getConsecutiveFailures(),
                    j.getLastError(), j.getCreatedAt());
        }
    }

    public record InstanceResponse(
            Long id, Long jobId, String status, Instant scheduledTime, Instant startedAt, Instant completedAt,
            Long durationMs, Integer httpStatusCode, String errorMessage, String responseBody, int retryCount,
            String workerId) {
        public static InstanceResponse of(JobInstance i) {
            return new InstanceResponse(i.getId(), i.getJob().getId(), i.getStatus().name(), i.getScheduledTime(),
                    i.getStartedAt(), i.getCompletedAt(), i.getDurationMs(), i.getHttpStatusCode(),
                    i.getErrorMessage(), i.getResponseBody(), i.getRetryCount(), i.getWorkerId());
        }
    }

    public record LogResponse(Long id, Long instanceId, Long jobId, String level, String message,
                              Map<String, Object> details, Instant createdAt) {
        public static LogResponse of(JobLog l) {
            return new LogResponse(l.getId(), l.getInstanceId(), l.getJobId(), l.getLogLevel().name(),
                    l.getMessage(), l.getDetails(), l.getCreatedAt());
        }
    }

    public record JobStatistics(Long jobId, long totalRuns, long successfulRuns, long failedRuns,
                                double successRate, long avgDurationMs, int consecutiveFailures,
                                Instant lastRunAt, Map<String, Long> instancesByStatus) {}

    public record PageResponse<T>(java.util.List<T> content, int page, int size, long totalElements, int totalPages) {
        public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> p) {
            return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }

    public record ErrorResponse(Instant timestamp, int status, String error, String message, Map<String, String> fields) {}
}
