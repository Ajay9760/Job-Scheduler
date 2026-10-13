package com.example.chronos.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.chronos")
public class WorkerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkerServiceApplication.class, args);
    }
}
