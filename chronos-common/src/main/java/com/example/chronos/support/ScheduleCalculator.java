package com.example.chronos.support;

import com.example.chronos.domain.enums.ScheduleType;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.scheduling.support.CronExpression;

/** Pure schedule maths. All results are UTC instants; cron is evaluated in the job's time zone. */
public final class ScheduleCalculator {

    private static final Pattern INTERVAL = Pattern.compile("^(\\d{1,9})([smhd])$");
    private static final Duration MIN_INTERVAL = Duration.ofSeconds(10);

    private ScheduleCalculator() {}

    /** @return the next fire time strictly after {@code after}, or null if the schedule is exhausted (ONCE already past). */
    public static Instant next(ScheduleType type, String expression, String timezone, Instant after) {
        switch (type) {
            case CRON: {
                ZonedDateTime z = parseCron(expression).next(after.atZone(zone(timezone)));
                return z == null ? null : z.toInstant();
            }
            case INTERVAL:
                return after.plus(parseInterval(expression));
            case ONCE: {
                Instant at = parseOnce(expression);
                return at.isAfter(after) ? at : null;
            }
            default:
                throw new IllegalArgumentException("Unsupported schedule type " + type);
        }
    }

    /** First fire time for a newly created job. For ONCE a time in the past fires immediately. */
    public static Instant first(ScheduleType type, String expression, String timezone, Instant now) {
        if (type == ScheduleType.ONCE) {
            Instant at = parseOnce(expression);
            return at.isAfter(now) ? at : now;
        }
        return next(type, expression, timezone, now);
    }

    /** Throws IllegalArgumentException with a human readable message if the expression is invalid. */
    public static void validate(ScheduleType type, String expression, String timezone) {
        zone(timezone);
        switch (type) {
            case CRON -> parseCron(expression);
            case INTERVAL -> parseInterval(expression);
            case ONCE -> parseOnce(expression);
        }
    }

    static CronExpression parseCron(String expr) {
        if (expr == null || expr.isBlank()) {
            throw new IllegalArgumentException("Cron expression is required");
        }
        String e = expr.trim().replaceAll("\\s+", " ");
        int fields = e.split(" ").length;
        if (fields == 5) {
            e = "0 " + e; // classic 5-field cron: add the seconds field
        } else if (fields != 6) {
            throw new IllegalArgumentException("Cron expression must have 5 or 6 fields: " + expr);
        }
        try {
            return CronExpression.parse(e);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid cron expression '" + expr + "': " + ex.getMessage());
        }
    }

    static Duration parseInterval(String expr) {
        Matcher m = expr == null ? null : INTERVAL.matcher(expr.trim().toLowerCase());
        if (m == null || !m.matches()) {
            throw new IllegalArgumentException("Interval must look like 30s, 5m, 2h or 1d: " + expr);
        }
        long n = Long.parseLong(m.group(1));
        Duration d = switch (m.group(2)) {
            case "s" -> Duration.ofSeconds(n);
            case "m" -> Duration.ofMinutes(n);
            case "h" -> Duration.ofHours(n);
            default -> Duration.ofDays(n);
        };
        if (d.compareTo(MIN_INTERVAL) < 0) {
            throw new IllegalArgumentException("Interval must be at least 10s");
        }
        return d;
    }

    static Instant parseOnce(String expr) {
        try {
            return Instant.parse(expr.trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("One-off schedule must be an ISO-8601 instant like 2030-01-01T10:00:00Z");
        }
    }

    static ZoneId zone(String tz) {
        try {
            return ZoneId.of(tz == null || tz.isBlank() ? "UTC" : tz);
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Unknown time zone: " + tz);
        }
    }
}
