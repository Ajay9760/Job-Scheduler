package com.example.chronos.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Scheduler: turns due jobs into PENDING instances (trigger), publishes PENDING instances to the broker
 * (dispatcher) and cleans up (reaper, retention). Safe to run N replicas: every step uses
 * SELECT ... FOR UPDATE SKIP LOCKED, so replicas never work on the same rows.
 */
@SpringBootApplication(scanBasePackages = "com.example.chronos")
@EnableScheduling
public class SchedulerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SchedulerServiceApplication.class, args);
    }
}
