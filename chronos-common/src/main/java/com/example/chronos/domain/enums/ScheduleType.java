package com.example.chronos.domain.enums;

public enum ScheduleType {
    /** Cron expression (5 or 6 fields) evaluated in the job's time zone. */
    CRON,
    /** Fixed delay between runs, e.g. {@code 30s}, {@code 5m}, {@code 2h}, {@code 1d}. */
    INTERVAL,
    /** Run once at an ISO-8601 instant. */
    ONCE
}
