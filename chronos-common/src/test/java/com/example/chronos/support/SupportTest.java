package com.example.chronos.support;

import static org.junit.jupiter.api.Assertions.*;

import com.example.chronos.domain.enums.ScheduleType;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SupportTest {
    private final Instant t0 = Instant.parse("2030-01-01T00:00:00Z");

    @Test
    void fiveFieldCronGetsSecondsField() {
        assertEquals(Instant.parse("2030-01-01T00:05:00Z"),
                ScheduleCalculator.next(ScheduleType.CRON, "*/5 * * * *", "UTC", t0));
    }

    @Test
    void cronHonoursTimezone() {
        // 09:00 in Kolkata (+05:30) is 03:30 UTC
        assertEquals(Instant.parse("2030-01-01T03:30:00Z"),
                ScheduleCalculator.next(ScheduleType.CRON, "0 9 * * *", "Asia/Kolkata", t0));
    }

    @Test
    void intervalAndOnce() {
        assertEquals(t0.plusSeconds(300), ScheduleCalculator.next(ScheduleType.INTERVAL, "5m", "UTC", t0));
        assertNull(ScheduleCalculator.next(ScheduleType.ONCE, "2029-01-01T00:00:00Z", "UTC", t0));
        assertEquals(t0, ScheduleCalculator.first(ScheduleType.ONCE, "2029-01-01T00:00:00Z", "UTC", t0));
    }

    @Test
    void invalidSchedulesRejected() {
        assertThrows(IllegalArgumentException.class, () -> ScheduleCalculator.validate(ScheduleType.CRON, "nope", "UTC"));
        assertThrows(IllegalArgumentException.class, () -> ScheduleCalculator.validate(ScheduleType.INTERVAL, "1s", "UTC"));
        assertThrows(IllegalArgumentException.class, () -> ScheduleCalculator.validate(ScheduleType.CRON, "* * * * *", "Mars/Base"));
    }

    @Test
    void ssrfGuardBlocksInternalTargets() {
        UrlSafetyValidator v = new UrlSafetyValidator(false);
        for (String u : new String[] {"http://127.0.0.1/x", "http://169.254.169.254/latest", "http://10.0.0.5/",
                "http://192.168.1.1/", "ftp://example.com/", "http://user:pw@1.1.1.1/", "http://[::1]/"}) {
            assertThrows(IllegalArgumentException.class, () -> v.validate(u), u);
        }
        assertDoesNotThrow(() -> v.validate("http://1.1.1.1/"));
        assertDoesNotThrow(() -> new UrlSafetyValidator(true).validate("http://127.0.0.1/"));
    }
}
