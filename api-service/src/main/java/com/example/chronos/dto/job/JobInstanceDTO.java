package com.example.chronos.dto.job;


import com.example.chronos.domain.JobInstance.InstanceStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobInstanceDTO {

    private Long id;
    private Long jobId;
    private String jobName;
    private InstanceStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;

    private Long durationMs;
    private Integer httpStatusCode;
    private String responseBody;
    private String errorMessage;
    private Integer retryCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // Computed fields
    private String statusColor;
    private String durationFormatted;

    public void computeDerivedFields() {
        this.statusColor = getStatusColor(status);
        this.durationFormatted = formatDuration(durationMs);
    }

    private String getStatusColor(InstanceStatus status) {
        return switch (status) {
            case SUCCESS -> "green";
            case FAILED, TIMEOUT -> "red";
            case RUNNING -> "blue";
            case PENDING -> "yellow";
            case CANCELLED -> "gray";
            case RETRY_PENDING -> null;
            case SKIPPED -> "orange";
        };
    }

    private String formatDuration(Long ms) {
        if (ms == null) return "N/A";
        if (ms < 1000) return ms + "ms";
        if (ms < 60000) return String.format("%.2fs", ms / 1000.0);
        long minutes = ms / 60000;
        long seconds = (ms % 60000) / 1000;
        return String.format("%dm %ds", minutes, seconds);
    }
}
