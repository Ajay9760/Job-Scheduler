package com.example.chronos.worker;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Clock;
import org.junit.jupiter.api.Test;

class HostCircuitBreakerTest {
    @Test
    void opensAfterThresholdAndClosesOnSuccess() {
        HostCircuitBreaker b = new HostCircuitBreaker(Clock.systemUTC(), 3, 60);
        b.failure("h");
        b.failure("h");
        assertNull(b.blockedUntil("h"));
        b.failure("h");
        assertNotNull(b.blockedUntil("h"));
        b.success("h");
        assertNull(b.blockedUntil("h"));
    }
}
