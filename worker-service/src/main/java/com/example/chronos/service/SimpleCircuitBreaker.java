package com.example.chronos.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimpleCircuitBreaker {

    private record State(int failures, Instant openedAt) {}

    private final Map<String, State> states = new ConcurrentHashMap<>();
    private final int failureThreshold = 3;
    private final long openSeconds = 60;

    public boolean isOpen(String key) {
        State s = states.get(key);
        if (s == null) return false;
        if (s.failures < failureThreshold) return false;
        if (s.openedAt().plusSeconds(openSeconds).isBefore(Instant.now())) {
            states.remove(key);
            return false;
        }
        return true;
    }

    public void onSuccess(String key) {
        states.remove(key);
    }

    public void onFailure(String key) {
        State prev = states.getOrDefault(key, new State(0, Instant.now()));
        int failures = prev.failures + 1;
        Instant openedAt = prev.openedAt();
        if (failures >= failureThreshold) {
            openedAt = Instant.now();
        }
        states.put(key, new State(failures, openedAt));
    }
}
