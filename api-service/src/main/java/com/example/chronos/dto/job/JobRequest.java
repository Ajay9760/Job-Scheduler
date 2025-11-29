package com.example.chronos.dto.job;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequest {

    @NotBlank(message = "Job name is required")
    @Size(min = 3, max = 255, message = "Job name must be between 3 and 255 characters")
    private String name;

    @NotBlank(message = "Job type is required")
    @Pattern(regexp = "HTTP_GET|HTTP_POST|HTTP_PUT|HTTP_DELETE", message = "Invalid job type")
    private String jobType;

    @NotBlank(message = "Schedule type is required")
    @Pattern(regexp = "CRON|ONE_TIME", message = "Schedule type must be CRON or ONE_TIME")
    private String scheduleType;

    private String cronExpression;

    @NotNull(message = "Payload is required")
    private Map<String, Object> payload;

    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 10, message = "Priority must not exceed 10")
    private Integer priority;

    @Min(value = 1, message = "Timeout must be at least 1 second")
    @Max(value = 300, message = "Timeout must not exceed 300 seconds")
    private Integer timeoutSeconds;
}