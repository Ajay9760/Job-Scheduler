package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.JobInstance;
import com.example.chronos.domain.JobInstance.InstanceStatus;
import com.example.chronos.domain.JobLog;
import com.example.chronos.domain.JobLog.LogLevel;
import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.domain.enums.ScheduleType;
import com.example.chronos.repository.JobInstanceRepository;
import com.example.chronos.repository.JobLogRepository;
import com.example.chronos.repository.JobRepository;
import com.example.chronos.support.ScheduleCalculator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * All state transitions of a job run live here, so scheduler, worker, reaper and API cannot drift apart.
 * Every method is a single DB transaction; the transitions themselves are conditional UPDATEs, so
 * running them twice (duplicate delivery, two replicas) is harmless.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JobLifecycleService {

    private static final long MAX_BACKOFF_SECONDS = 3600;

    private final JobRepository jobs;
    private final JobInstanceRepository instances;
    private final JobLogRepository logs;
    private final Clock clock;

    /** Initial next_run_at for a new or resumed job. */
    public Instant firstRun(Job job) {
        return ScheduleCalculator.first(job.getScheduleType(), job.getCronExpression(), job.getTimezone(), clock.instant());
    }

    /** Create the PENDING row (the outbox entry) for one planned run. */
    @Transactional
    public JobInstance createInstance(Job job, Instant scheduledTime, int retryCount) {
        JobInstance i = JobInstance.builder()
                .job(job)
                .scheduledTime(scheduledTime.truncatedTo(ChronoUnit.MICROS))
                .retryCount(retryCount)
                .build();
        return instances.saveAndFlush(i);
    }

    /** Manual trigger: runs now, outside the schedule, without touching next_run_at. */
    @Transactional
    public JobInstance trigger(Job job) {
        JobInstance i = createInstance(job, clock.instant(), 0);
        log(job.getId(), i.getId(), LogLevel.INFO, "Manually triggered", null);
        return i;
    }

    /**
     * Called by the scheduler for a job it holds a row lock on: creates the instance for the due slot
     * and advances next_run_at. Missed slots (scheduler down for an hour) fire ONCE and then jump to the
     * next future slot ("fire once on misfire") rather than replaying a backlog.
     */
    @Transactional
    public void fireDueJob(Job job) {
        Instant now = clock.instant();
        Instant slot = job.getNextRunAt();
        JobInstance i = createInstance(job, slot, 0);
        log(job.getId(), i.getId(), LogLevel.INFO, "Scheduled run created", Map.of("slot", slot.toString()));

        Instant next = job.getScheduleType() == ScheduleType.ONCE
                ? null
                : ScheduleCalculator.next(job.getScheduleType(), job.getCronExpression(), job.getTimezone(), now);
        job.setNextRunAt(next);
        if (next == null) {
            job.setStatus(JobStatus.COMPLETED);
        }
        jobs.save(job);
    }

    /** Worker finished (or gave up on) a RUNNING instance. Records stats and schedules a retry if allowed. */
    @Transactional
    public void finish(Long instanceId, InstanceStatus to, Long durationMs, Integer httpStatus, String body, String error) {
        Instant now = clock.instant();
        int changed = instances.completeRunning(instanceId, to, now, durationMs, httpStatus, trim(body, 10_000), trim(error, 4_000));
        if (changed == 0) {
            log.info("Instance {} was no longer RUNNING; ignoring late result", instanceId);
            return;
        }
        JobInstance inst = instances.findWithJobById(instanceId).orElse(null);
        if (inst == null) {
            return;
        }
        Job job = inst.getJob();
        if (to == InstanceStatus.SUCCESS) {
            jobs.recordSuccess(job.getId(), now, durationMs == null ? 0 : durationMs);
            log(job.getId(), instanceId, LogLevel.INFO, "Run succeeded", Map.of("httpStatus", String.valueOf(httpStatus)));
            return;
        }
        jobs.recordFailure(job.getId(), now, trim(error, 2_000));
        log(job.getId(), instanceId, LogLevel.ERROR, "Run " + to + ": " + error, null);
        scheduleRetry(job, inst, now);
    }

    private void scheduleRetry(Job job, JobInstance failed, Instant now) {
        if (job.getStatus() == JobStatus.CANCELLED || failed.getRetryCount() >= job.getMaxRetries()) {
            return;
        }
        long delay = Math.min(MAX_BACKOFF_SECONDS, job.getBackoffSeconds() * (1L << Math.min(failed.getRetryCount(), 20)));
        // The retry is just another PENDING row with a future scheduled_time: it survives restarts.
        JobInstance retry = createInstance(job, now.plus(Duration.ofSeconds(delay)), failed.getRetryCount() + 1);
        log(job.getId(), retry.getId(), LogLevel.WARN, "Retry " + retry.getRetryCount() + "/" + job.getMaxRetries()
                + " scheduled in " + delay + "s", null);
    }

    /** Worker claims a PENDING/QUEUED instance. False means someone else has it or it is already done. */
    @Transactional
    public boolean claim(Long instanceId, String workerId) {
        return instances.claim(instanceId, workerId, clock.instant()) == 1;
    }

    /** Put a RUNNING instance back to PENDING for later without consuming a retry (circuit open). */
    @Transactional
    public void defer(Long instanceId, Instant until, String reason) {
        if (instances.deferRunning(instanceId, until, clock.instant()) == 1) {
            instances.findWithJobById(instanceId).ifPresent(i ->
                    log(i.getJob().getId(), instanceId, LogLevel.WARN, "Deferred until " + until + ": " + reason, null));
        }
    }

    /** Reaper path: a RUNNING instance whose worker died or exceeded timeout. */
    @Transactional
    public void failTimedOut(Long instanceId) {
        finish(instanceId, InstanceStatus.TIMEOUT, null, null, null, "Worker did not report a result before the deadline");
    }

    @Transactional
    public void log(Long jobId, Long instanceId, LogLevel level, String message, Map<String, Object> details) {
        logs.save(JobLog.of(jobId, instanceId, level, message, details));
    }

    private static String trim(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
