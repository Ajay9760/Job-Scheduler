package com.example.chronos.service;

import com.example.chronos.domain.JobInstance;
import com.example.chronos.repository.JobINstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobInstancePoller {

    private final JobINstanceRepository instanceRepository;
    private final JobInstanceService jobInstanceService;

    @Scheduled(fixedDelay = 5000) // Poll every 5 seconds
    public void pollPendingInstances() {
        try {
            List<JobInstance> pending = instanceRepository
                    .findPendingInstancesReadyToRun(LocalDateTime.now());

            log.info("Found {} pending instances", pending.size());

            for (JobInstance instance : pending) {
                try {
                    log.info("Queueing instance {} for execution", instance.getId());
                    jobInstanceService.sendToWorkerQueue(instance);
                } catch (Exception e) {
                    log.error("Failed to queue instance {}: {}", instance.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error polling instances: {}", e.getMessage());
        }
    }
}