package com.example.chronos.scheduler;

import com.example.chronos.domain.Job;
import com.example.chronos.repository.JobRepository;
import com.example.chronos.service.JobLifecycleService;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Step 1: due jobs -> PENDING instances, next_run_at advanced, all in one transaction per batch. */
@Component
@Slf4j
@RequiredArgsConstructor
public class TriggerService {

    private final JobRepository jobs;
    private final JobLifecycleService lifecycle;
    private final TransactionTemplate tx;
    private final Clock clock;

    @Value("${chronos.scheduler.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${chronos.scheduler.trigger-interval-ms:1000}")
    public void tick() {
        int n;
        do {
            n = tx.execute(status -> {
                List<Job> due = jobs.lockDueJobs(clock.instant(), batchSize);
                for (Job j : due) {
                    try {
                        lifecycle.fireDueJob(j);
                    } catch (RuntimeException e) {
                        // Never let one bad job block the batch or roll back its neighbours.
                        log.error("Could not fire job {}: {}", j.getId(), e.toString());
                        j.setNextRunAt(clock.instant().plusSeconds(60));
                        jobs.save(j);
                    }
                }
                return due.size();
            });
        } while (n >= batchSize); // drain a backlog without waiting for the next tick
    }
}
