package com.example.chronos.dto.job;


import com.example.chronos.domain.JobLog.LogLevel;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobLogDTO {

    private Long id;
    private Long instanceId;
    private Long jobId;
    private LogLevel logLevel;
    private String message;
    private Map<String, Object> details;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    private String levelColor;
    private String levelIcon;

    public void computeUIFields() {
        this.levelColor = getLevelColor(logLevel);
        this.levelIcon = getLevelIcon(logLevel);
    }

    private String getLevelColor(LogLevel level) {
        return switch (level) {
            case DEBUG -> "gray";
            case INFO -> "blue";
            case WARN -> "orange";
            case ERROR -> "red";
        };
    }

    private String getLevelIcon(LogLevel level) {
        return switch (level) {
            case DEBUG -> "🔍";
            case INFO -> "ℹ️";
            case WARN -> "⚠️";
            case ERROR -> "❌";
        };
    }
}
