package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.JobInstance;
import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.repository.JobInstanceRepository;
import com.example.chronos.repository.JobLogRepository;
import com.example.chronos.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerManagementService {

    private final JobRepository jobRepository;
    private final JobInstanceRepository instanceRepository;
    private final JobLogRepository logRepository;

    /**
     * Get scheduler status
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();

        long totalJobs = jobRepository.count();

        // Count by status
        long scheduledJobs = jobRepository.findAll().stream()
                .filter(j -> j.getStatus() == JobStatus.SCHEDULED)
                .count();
        long pausedJobs = jobRepository.findAll().stream()
                .filter(j -> j.getStatus() == JobStatus.PAUSED)
                .count();

        // Count instances
        List<JobInstance> allInstances = instanceRepository.findAll();
        long pendingInstances = allInstances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.PENDING)
                .count();
        long runningInstances = allInstances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.RUNNING)
                .count();

        status.put("totalJobs", totalJobs);
        status.put("scheduledJobs", scheduledJobs);
        status.put("pausedJobs", pausedJobs);
        status.put("pendingInstances", pendingInstances);
        status.put("runningInstances", runningInstances);
        status.put("schedulerRunning", true);
        status.put("timestamp", Instant.now());

        return status;
    }

    /**
     * Force schedule all active jobs
     */
    @Transactional
    public int forceScheduleAllJobs() {
        List<Job> scheduledJobs = jobRepository.findAll().stream()
                .filter(j -> j.getStatus() == JobStatus.SCHEDULED)
                .toList();

        int count = 0;

        for (Job job : scheduledJobs) {
            try {
                // Create immediate instance
                JobInstance instance = JobInstance.builder()
                        .job(job)
                        .status(JobInstance.InstanceStatus.PENDING)
                        .scheduledTime(LocalDateTime.now())
                        .retryCount(0)
                        .build();

                instanceRepository.save(instance);
                count++;
                log.info("Force scheduled job: {} (ID: {})", job.getName(), job.getId());
            } catch (Exception e) {
                log.error("Failed to schedule job {}: {}", job.getId(), e.getMessage());
            }
        }

        log.info("Force scheduled {} jobs", count);
        return count;
    }

    /**
     * Calculate next run times for all jobs
     */
    @Transactional
    public int calculateAllNextRuns() {
        List<Job> jobs = jobRepository.findAll().stream()
                .filter(j -> j.getStatus() == JobStatus.SCHEDULED)
                .toList();

        int updated = 0;

        for (Job job : jobs) {
            try {
                LocalDateTime nextRun = LocalDateTime.now().plusHours(1); // Example: schedule 1 hour from now
                job.setNextRunAt(nextRun);

                jobRepository.save(job);
                updated++;
            } catch (Exception e) {
                log.error("Failed to calculate next run for job {}: {}", job.getId(), e.getMessage());
            }
        }

        log.info("Calculated next run for {} jobs", updated);
        return updated;
    }

    /**
     * Cleanup old data
     */
    @Transactional
    public Map<String, Integer> cleanup(int olderThanDays) {
        Map<String, Integer> result = new HashMap<>();

        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);

        // Cleanup old instances
        List<JobInstance> oldInstances = instanceRepository.findAll().stream()
                .filter(i -> i.getCreatedAt() != null && i.getCreatedAt().isBefore(cutoff))
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.SUCCESS
                        || i.getStatus() == JobInstance.InstanceStatus.FAILED)
                .toList();

        instanceRepository.deleteAll(oldInstances);
        result.put("instancesDeleted", oldInstances.size());
        log.info("Deleted {} old instances", oldInstances.size());

        // Cleanup old logs
        try {
            logRepository.deleteByCreatedAtBefore(cutoff);
            result.put("logsDeleted", 0); // Can't easily count deleted
        } catch (Exception e) {
            log.warn("Failed to cleanup logs: {}", e.getMessage());
            result.put("logsDeleted", 0);
        }

        return result;
    }
}