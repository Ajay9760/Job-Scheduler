package com.example.chronos.dto.job;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatisticsDTO {

    private Long jobId;
    private String jobName;

    // Execution counts
    private Long totalRuns;
    private Long successfulRuns;
    private Long failedRuns;
    private Long pendingRuns;
    private Long runningRuns;

    // Success rate
    private Double successRate;

    // Duration statistics
    private Long avgDurationMs;
    private Long minDurationMs;
    private Long maxDurationMs;
    private String avgDurationFormatted;

    // Recent activity
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastRunAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastSuccessAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastFailureAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime nextRunAt;

    // Error information
    private String lastErrorMessage;
    private Integer consecutiveFailures;

    // Historical data for charts
    private List<DailyStatistic> dailyStats;
    private Map<String, Long> statusDistribution;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStatistic {
        private String date;
        private Long total;
        private Long success;
        private Long failed;
        private Long avgDuration;
    }

    public void calculateSuccessRate() {
        if (totalRuns != null && totalRuns > 0) {
            this.successRate = (successfulRuns.doubleValue() / totalRuns) * 100;
        } else {
            this.successRate = 0.0;
        }
    }

    public void formatDuration() {
        this.avgDurationFormatted = formatMs(avgDurationMs);
    }

    private String formatMs(Long ms) {
        if (ms == null) return "N/A";
        if (ms < 1000) return ms + "ms";
        if (ms < 60000) return String.format("%.2fs", ms / 1000.0);
        long minutes = ms / 60000;
        long seconds = (ms % 60000) / 1000;
        return String.format("%dm %ds", minutes, seconds);
    }
}