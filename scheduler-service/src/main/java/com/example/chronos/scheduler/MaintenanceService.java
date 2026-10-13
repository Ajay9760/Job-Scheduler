package com.example.chronos.scheduler;

import com.example.chronos.repository.JobInstanceRepository;
import com.example.chronos.repository.JobLogRepository;
import com.example.chronos.service.JobLifecycleService;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** Reaper (stuck RUNNING / stale QUEUED instances) and batched retention deletes. */
@Component
@Slf4j
@RequiredArgsConstructor
public class MaintenanceService {

    private final JobInstanceRepository instances;
    private final JobLogRepository logs;
    private final JobLifecycleService lifecycle;
    private final TransactionTemplate tx;
    private final Clock clock;

    @Value("${chronos.reaper.grace-seconds:60}") private int graceSeconds;
    @Value("${chronos.reaper.stale-queued-seconds:300}") private long staleQueuedSeconds;
    @Value("${chronos.retention.days:30}") private long retentionDays;

    /** A worker that crashed mid-run leaves RUNNING rows behind; fail them (which may schedule a retry). */
    @Scheduled(fixedDelayString = "${chronos.reaper.interval-ms:30000}")
    public void reap() {
        List<Long> stuck = instances.findTimedOutRunning(clock.instant(), graceSeconds, 200);
        for (Long id : stuck) {
            try {
                lifecycle.failTimedOut(id);
            } catch (RuntimeException e) {
                log.error("Reaper could not fail instance {}: {}", id, e.toString());
            }
        }
        // Message lost / broker wiped: put long-QUEUED rows back to PENDING so they get re-dispatched.
        Integer requeued = tx.execute(s -> instances.requeueStaleQueued(
                clock.instant().minusSeconds(staleQueuedSeconds), clock.instant()));
        if (!stuck.isEmpty() || (requeued != null && requeued > 0)) {
            log.warn("Reaper: failed {} stuck runs, re-queued {} stale messages", stuck.size(), requeued);
        }
    }

    @Scheduled(cron = "${chronos.retention.cron:0 30 3 * * *}")
    public void retention() {
        var cutoff = clock.instant().minus(Duration.ofDays(retentionDays));
        int deleted;
        do { // small batches keep locks and WAL bursts short
            Integer d = tx.execute(s -> instances.deleteOldTerminal(cutoff, 5_000));
            deleted = d == null ? 0 : d;
        } while (deleted > 0);
        do {
            Integer d = tx.execute(s -> logs.deleteOld(cutoff, 5_000));
            deleted = d == null ? 0 : d;
        } while (deleted > 0);
    }
}
