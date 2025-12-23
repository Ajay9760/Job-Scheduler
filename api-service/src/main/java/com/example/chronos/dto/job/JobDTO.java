package com.example.chronos.dto.job;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDTO {

    private Long id;

    @NotBlank(message = "Job name is required")
    private String name;

    private String description;

    @NotBlank(message = "HTTP URL is required")
    private String httpUrl;

    @NotBlank(message = "HTTP method is required")
    private String httpMethod;

    private Map<String, String> httpHeaders;

    private String httpBody;

    @NotBlank(message = "Schedule type is required")
    private String scheduleType;

    @NotBlank(message = "CRON expression is required")
    private String cronExpression;

    private String status;

    private Integer timeoutSeconds;

    // Retry configuration
    private Integer maxRetries;
    private String backoffStrategy;

    private String webhookUrl;

    // Statistics fields
    private Integer totalRuns;
    private Integer successfulRuns;
    private Integer failedRuns;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastRunAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime nextRunAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
