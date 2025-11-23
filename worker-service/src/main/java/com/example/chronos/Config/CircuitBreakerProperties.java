package com.example.chronos.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "circuit-breaker")
public class CircuitBreakerProperties {

    private int failureThreshold = 3;
    private long openDurationSeconds = 60;

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public long getOpenDurationSeconds() {
        return openDurationSeconds;
    }

    public void setOpenDurationSeconds(long openDurationSeconds) {
        this.openDurationSeconds = openDurationSeconds;
    }
}

