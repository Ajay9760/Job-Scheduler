package com.example.chronos.worker;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Per-target-host breaker so one dead endpoint cannot eat every worker thread. When open, work is
 * DEFERRED (put back as PENDING for later), never dropped and never counted as a failure.
 * State is per worker instance, which is fine: it is a protection, not a source of truth.
 */
@Component
public class HostCircuitBreaker {

    private record State(int failures, Instant openUntil) {}

    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int threshold;
    private final Duration openFor;

    public HostCircuitBreaker(Clock clock,
                              @Value("${chronos.worker.breaker.threshold:5}") int threshold,
                              @Value("${chronos.worker.breaker.open-seconds:60}") long openSeconds) {
        this.clock = clock;
        this.threshold = threshold;
        this.openFor = Duration.ofSeconds(openSeconds);
    }

    /** @return the instant until which the host is blocked, or null if calls are allowed. */
    public Instant blockedUntil(String host) {
        State s = states.get(host);
        return s != null && s.openUntil() != null && s.openUntil().isAfter(clock.instant()) ? s.openUntil() : null;
    }

    public void success(String host) {
        states.remove(host);
    }

    public void failure(String host) {
        states.merge(host, new State(1, null), (old, n) -> {
            int f = old.failures() + 1;
            return new State(f, f >= threshold ? clock.instant().plus(openFor) : null);
        });
    }
}
