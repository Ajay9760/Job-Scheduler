package com.example.chronos.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@Slf4j
public class CronCalculator {

    // Calculate next run time based on schedule type and expression

    public LocalDateTime calculateNextRun(String scheduleType, String cronExpression, LocalDateTime currentTime) {
        if (currentTime == null) {
            currentTime = LocalDateTime.now();
        }

        return switch (scheduleType.toUpperCase()) {
            case "CRON" -> calculateCronNextRun(cronExpression, currentTime);
            case "INTERVAL" -> calculateIntervalNextRun(cronExpression, currentTime);
            case "ONCE" -> null; // One-time jobs don't have next run
            default -> throw new IllegalArgumentException("Unknown schedule type: " + scheduleType);
        };
    }

    //Calculate next run for CRON expressions

    private LocalDateTime calculateCronNextRun(String cronExpression, LocalDateTime from) {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            var zonedDateTime = from.atZone(ZoneId.systemDefault());
            var next = cron.next(zonedDateTime);

            if (next == null) {
                log.warn("No next execution time for CRON: {}", cronExpression);
                return null;
            }

            LocalDateTime nextRun = next.toLocalDateTime();
            log.debug("CRON {} next run: {}", cronExpression, nextRun);
            return nextRun;
        } catch (Exception e) {
            log.error("Failed to parse CRON expression '{}': {}", cronExpression, e.getMessage());
            throw new IllegalArgumentException("Invalid CRON expression: " + cronExpression, e);
        }
    }

    //Calculate next run for interval-based schedules (e.g., "5m", "1h", "30s")

    private LocalDateTime calculateIntervalNextRun(String interval, LocalDateTime from) {
        try {
            Duration duration = parseInterval(interval);
            LocalDateTime nextRun = from.plus(duration);
            log.debug("Interval {} next run: {}", interval, nextRun);
            return nextRun;
        } catch (Exception e) {
            log.error("Failed to parse interval '{}': {}", interval, e.getMessage());
            throw new IllegalArgumentException("Invalid interval format: " + interval, e);
        }
    }

    // Parse interval string (e.g., "5m", "1h", "30s") to Duration

    private Duration parseInterval(String interval) {
        if (interval == null || interval.isEmpty()) {
            throw new IllegalArgumentException("Interval cannot be empty");
        }

        interval = interval.trim().toLowerCase();

        int i = 0;
        while (i < interval.length() && (Character.isDigit(interval.charAt(i)) || interval.charAt(i) == '.')) {
            i++;
        }

        if (i == 0) {
            throw new IllegalArgumentException("Interval must start with a number");
        }

        String numberPart = interval.substring(0, i);
        String unitPart = interval.substring(i);

        long value = Long.parseLong(numberPart);

        return switch (unitPart) {
            case "s", "sec", "second", "seconds" -> Duration.ofSeconds(value);
            case "m", "min", "minute", "minutes" -> Duration.ofMinutes(value);
            case "h", "hr", "hour", "hours" -> Duration.ofHours(value);
            case "d", "day", "days" -> Duration.ofDays(value);
            default -> throw new IllegalArgumentException("Unknown interval unit: " + unitPart);
        };
    }

    //Validate CRON expression

    public boolean isValidCron(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Validate interval format

    public boolean isValidInterval(String interval) {
        try {
            parseInterval(interval);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Get human-readable description of schedule

    public String getScheduleDescription(String scheduleType, String expression) {
        try {
            return switch (scheduleType.toUpperCase()) {
                case "CRON" -> describeCron(expression);
                case "INTERVAL" -> "Every " + expression;
                case "ONCE" -> "One-time execution";
                default -> "Unknown schedule";
            };
        } catch (Exception e) {
            return "Invalid schedule: " + e.getMessage();
        }
    }

    // Get human-readable CRON description

    private String describeCron(String cronExpression) {
         return switch (cronExpression) {
            case "0 * * * * *" -> "Every minute";
            case "0 0 * * * *" -> "Every hour";
            case "0 0 0 * * *" -> "Every day at midnight";
            case "0 0 12 * * *" -> "Every day at noon";
            case "0 0 0 * * MON" -> "Every Monday at midnight";
            case "0 0 9 * * MON-FRI" -> "Weekdays at 9 AM";
            default -> "CRON: " + cronExpression;
        };
    }

    // Calculate time until next run

    public Duration getTimeUntilNextRun(String scheduleType, String expression, LocalDateTime lastRun) {
        LocalDateTime nextRun = calculateNextRun(scheduleType, expression, lastRun);
        if (nextRun == null) {
            return null;
        }
        return Duration.between(LocalDateTime.now(), nextRun);
    }
}