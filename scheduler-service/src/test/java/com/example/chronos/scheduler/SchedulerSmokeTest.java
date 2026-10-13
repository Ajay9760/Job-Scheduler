package com.example.chronos.scheduler;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class SchedulerSmokeTest {
    @Test
    void applicationClassLoads() {
        assertNotNull(SchedulerServiceApplication.class);
    }
}
