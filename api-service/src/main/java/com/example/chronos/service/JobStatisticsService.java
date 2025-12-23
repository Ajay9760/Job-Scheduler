package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.JobInstance;
import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.dto.job.JobStatisticsDTO;
import com.example.chronos.repository.JobInstanceRepository;
import com.example.chronos.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobStatisticsService {

    private final JobRepository jobRepository;
    private final JobInstanceRepository instanceRepository;

    //  Get job statistics
    @Transactional(readOnly = true)
    public Map<String, Object> getJobStatistics(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        var allInstances = instanceRepository
                .findByJob_Id(jobId, Pageable.unpaged())
                .getContent();

        long totalRuns = allInstances.size();
        long successfulRuns = allInstances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.COMPLETED)
                .count();
        long failedRuns = allInstances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.FAILED)
                .count();
        long pendingRuns = allInstances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.PENDING)
                .count();
        long runningRuns = allInstances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.RUNNING)
                .count();

        // Average duration
        OptionalDouble avgDuration = allInstances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.COMPLETED
                        && i.getDurationMs() != null)
                .mapToLong(JobInstance::getDurationMs)
                .average();

        Long avgDurationMs = avgDuration.isPresent() ? (long) avgDuration.getAsDouble() : 0L;

        // Success rate
        double successRate = totalRuns > 0
                ? (successfulRuns * 100.0 / totalRuns)
                : 0.0;

        int consecutiveFailures = calculateConsecutiveFailures(allInstances);

        // Build response map
        Map<String, Object> stats = new HashMap<>();
        stats.put("jobId", jobId);
        stats.put("jobName", job.getName());
        stats.put("totalRuns", totalRuns);
        stats.put("successfulRuns", successfulRuns);
        stats.put("failedRuns", failedRuns);
        stats.put("pendingRuns", pendingRuns);
        stats.put("runningRuns", runningRuns);
        stats.put("avgDurationMs", avgDurationMs);
        stats.put("successRate", String.format("%.2f%%", successRate));
        stats.put("consecutiveFailures", consecutiveFailures);

        if (job.getNextRunAt() != null) {
            stats.put("nextRunAt", job.getNextRunAt());
        }

        if (job.getLastError() != null) {
            stats.put("lastErrorMessage", job.getLastError());
        }

        return stats;
    }

    // Calculate and return comprehensive statistics for a job as DTO

    @Transactional(readOnly = true)
    public JobStatisticsDTO calculateJobStatistics(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        var allInstances = instanceRepository
                .findByJob_Id(jobId, Pageable.unpaged())
                .getContent();

        long totalRuns = allInstances.size();
        long successfulRuns = allInstances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.COMPLETED)
                .count();
        long failedRuns = allInstances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.FAILED)
                .count();
        long pendingRuns = allInstances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.PENDING)
                .count();
        long runningRuns = allInstances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.RUNNING)
                .count();

        // Average duration
        OptionalDouble avgDuration = allInstances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.COMPLETED
                        && i.getDurationMs() != null)
                .mapToLong(JobInstance::getDurationMs)
                .average();

        Long avgDurationMs = avgDuration.isPresent() ? (long) avgDuration.getAsDouble() : 0L;

        // Consecutive failures
        int consecutiveFailures = calculateConsecutiveFailures(allInstances);

        // Build DTO
        JobStatisticsDTO stats = JobStatisticsDTO.builder()
                .jobId(jobId)
                .jobName(job.getName())
                .totalRuns(totalRuns)
                .successfulRuns(successfulRuns)
                .failedRuns(failedRuns)
                .pendingRuns(pendingRuns)
                .runningRuns(runningRuns)
                .avgDurationMs(avgDurationMs)
                .nextRunAt(job.getNextRunAt())
                .lastErrorMessage(job.getLastError())
                .consecutiveFailures(consecutiveFailures)
                .build();

        stats.calculateSuccessRate();
        stats.formatDuration();

        return stats;
    }

    // Helper method to calculate consecutive failures

    private int calculateConsecutiveFailures(List<JobInstance> instances) {
        int consecutiveFailures = 0;

        List<JobInstance> sortedByCreated = instances.stream()
                .filter(i -> i.getCreatedAt() != null)
                .sorted(Comparator.comparing(JobInstance::getCreatedAt).reversed())
                .collect(Collectors.toList());

        for (JobInstance inst : sortedByCreated) {
            if (inst.getStatus() == JobInstance.InstanceStatus.FAILED) {
                consecutiveFailures++;
            } else {
                break;
            }
        }

        return consecutiveFailures;
    }

    // Get dashboard statistics

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalJobs = jobRepository.count();
        long activeJobs = jobRepository.findByStatus(JobStatus.SCHEDULED).size();

        var allInstances = instanceRepository.findAll();
        long totalExecutions = allInstances.size();
        long runningExecutions = allInstances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.RUNNING)
                .count();

        stats.put("totalJobs", totalJobs);
        stats.put("activeJobs", activeJobs);
        stats.put("totalExecutions", totalExecutions);
        stats.put("runningExecutions", runningExecutions);

        // Last 24 hours stats
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        long executions24h = allInstances.stream()
                .filter(i -> i.getCreatedAt() != null && i.getCreatedAt().isAfter(last24Hours))
                .count();
        long failures24h = allInstances.stream()
                .filter(i -> i.getCreatedAt() != null
                        && i.getCreatedAt().isAfter(last24Hours)
                        && i.getStatus() == JobInstance.InstanceStatus.FAILED)
                .count();

        stats.put("executions24h", executions24h);
        stats.put("failures24h", failures24h);

        return stats;
    }

    //Get job health metrics

    @Transactional(readOnly = true)
    public Map<String, Object> getJobHealth(Long jobId) {
        JobStatisticsDTO stats = calculateJobStatistics(jobId);

        Map<String, Object> health = new HashMap<>();
        health.put("successRate", stats.getSuccessRate());
        health.put("consecutiveFailures", stats.getConsecutiveFailures());
        health.put("avgDurationMs", stats.getAvgDurationMs());

        double healthScore = calculateHealthScore(stats);
        health.put("healthScore", healthScore);
        health.put("healthStatus", getHealthStatus(healthScore));

        return health;
    }

    // Get daily statistics

    @Transactional(readOnly = true)
    public List<JobStatisticsDTO.DailyStatistic> getDailyStatistics(Long jobId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        var instances = instanceRepository.findByScheduledTimeBetween(startDate, LocalDateTime.now());

        Map<LocalDate, List<JobInstance>> byDate = instances.stream()
                .filter(i -> i.getJob().getId().equals(jobId))
                .collect(Collectors.groupingBy(i -> i.getScheduledTime().toLocalDate()));

        return byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    List<JobInstance> dayInstances = entry.getValue();
                    long total = dayInstances.size();
                    long success = dayInstances.stream()
                            .filter(i -> i.getStatus() == JobInstance.InstanceStatus.COMPLETED)
                            .count();
                    long failed = dayInstances.stream()
                            .filter(i -> i.getStatus() == JobInstance.InstanceStatus.FAILED)
                            .count();

                    OptionalDouble avgDuration = dayInstances.stream()
                            .filter(i -> i.getDurationMs() != null)
                            .mapToLong(JobInstance::getDurationMs)
                            .average();

                    return JobStatisticsDTO.DailyStatistic.builder()
                            .date(entry.getKey().toString())
                            .total(total)
                            .success(success)
                            .failed(failed)
                            .avgDuration(avgDuration.isPresent() ? (long) avgDuration.getAsDouble() : 0L)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // Get hourly statistics

    @Transactional(readOnly = true)
    public Map<Integer, Long> getHourlyStatistics(Long jobId) {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        var instances = instanceRepository.findByScheduledTimeBetween(last24Hours, LocalDateTime.now());

        return instances.stream()
                .filter(i -> i.getJob().getId().equals(jobId))
                .collect(Collectors.groupingBy(
                        i -> i.getScheduledTime().getHour(),
                        Collectors.counting()
                ));
    }

    //Get execution trends

    @Transactional(readOnly = true)
    public Map<String, Object> getExecutionTrends(Long jobId, int days) {
        List<JobStatisticsDTO.DailyStatistic> dailyStats = getDailyStatistics(jobId, days);

        Map<String, Object> trends = new HashMap<>();
        trends.put("dailyStats", dailyStats);

        if (dailyStats.size() >= 2) {
            double recentAvg = dailyStats.subList(Math.max(dailyStats.size() - 3, 0), dailyStats.size()).stream()
                    .mapToLong(JobStatisticsDTO.DailyStatistic::getTotal)
                    .average().orElse(0);
            double olderAvg = dailyStats.subList(0, Math.min(3, dailyStats.size())).stream()
                    .mapToLong(JobStatisticsDTO.DailyStatistic::getTotal)
                    .average().orElse(0);

            trends.put("trend", recentAvg > olderAvg ? "up" : recentAvg < olderAvg ? "down" : "stable");
        }

        return trends;
    }

    // Get performance metrics

    @Transactional(readOnly = true)
    public Map<String, Object> getPerformanceMetrics(Long jobId) {
        var instances = instanceRepository
                .findByJob_Id(jobId, Pageable.unpaged())
                .getContent();

        var successfulInstances = instances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.COMPLETED && i.getDurationMs() != null)
                .collect(Collectors.toList());

        Map<String, Object> metrics = new HashMap<>();

        if (!successfulInstances.isEmpty()) {
            LongSummaryStatistics durationStats = successfulInstances.stream()
                    .mapToLong(JobInstance::getDurationMs)
                    .summaryStatistics();

            metrics.put("minDurationMs", durationStats.getMin());
            metrics.put("maxDurationMs", durationStats.getMax());
            metrics.put("avgDurationMs", (long) durationStats.getAverage());
            metrics.put("totalExecutions", durationStats.getCount());
        } else {
            metrics.put("minDurationMs", 0);
            metrics.put("maxDurationMs", 0);
            metrics.put("avgDurationMs", 0);
            metrics.put("totalExecutions", 0);
        }

        return metrics;
    }

    // Get system-wide statistics

    @Transactional(readOnly = true)
    public Map<String, Object> getSystemStatistics() {
        var allJobs = jobRepository.findAll();
        var allInstances = instanceRepository.findAll();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobs", allJobs.size());
        stats.put("totalExecutions", allInstances.size());

        long successCount = allInstances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.COMPLETED)
                .count();
        stats.put("totalSuccesses", successCount);

        long failureCount = allInstances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.FAILED)
                .count();
        stats.put("totalFailures", failureCount);

        return stats;
    }

    // Get top failing jobs

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopFailingJobs(int limit) {
        List<Map<String, Object>> jobStats = jobRepository.findAll().stream()
                .map(job -> {
                    var instances = instanceRepository
                            .findByJob_Id(job.getId(), Pageable.unpaged())
                            .getContent();

                    long totalRuns = instances.size();
                    long failedRuns = instances.stream()
                            .filter(i -> i.getStatus() == JobInstance.InstanceStatus.FAILED)
                            .count();

                    Map<String, Object> map = new HashMap<>();
                    map.put("jobId", job.getId());
                    map.put("jobName", job.getName());
                    map.put("failedRuns", failedRuns);
                    map.put("totalRuns", totalRuns);
                    return map;
                })
                .collect(Collectors.toList());

        return jobStats.stream()
                .sorted(Comparator.comparing(
                                (Map<String, Object> m) -> (Long) m.get("failedRuns"))
                        .reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    // Get slowest jobs
     @Transactional(readOnly = true)
    public List<Map<String, Object>> getSlowestJobs(int limit) {
        return jobRepository.findAll().stream()
                .map(job -> {
                    var instances = instanceRepository
                            .findByJob_Id(job.getId(), Pageable.unpaged())
                            .getContent();

                    Double avgDuration = instances.stream()
                            .filter(instance -> instance.getDurationMs() != null)
                            .mapToLong(JobInstance::getDurationMs)
                            .average()
                            .orElse(0.0);

                    Map<String, Object> map = new HashMap<>();
                    map.put("jobId", job.getId());
                    map.put("jobName", job.getName());
                    map.put("avgDurationMs", avgDuration);
                    return Map.entry(avgDuration, map);
                })
                .filter(entry -> entry.getKey() > 0)
                .sorted((e1, e2) -> Double.compare(e2.getKey(), e1.getKey()))
                .limit(limit)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    // Get duration distribution

    @Transactional(readOnly = true)
    public Map<String, Long> getDurationDistribution(Long jobId) {
        var instances = instanceRepository
                .findByJob_Id(jobId, Pageable.unpaged())
                .getContent();

        Map<String, Long> distribution = new HashMap<>();
        distribution.put("0-1s", instances.stream()
                .filter(i -> i.getDurationMs() != null && i.getDurationMs() < 1000)
                .count());
        distribution.put("1-5s", instances.stream()
                .filter(i -> i.getDurationMs() != null && i.getDurationMs() >= 1000 && i.getDurationMs() < 5000)
                .count());
        distribution.put("5-10s", instances.stream()
                .filter(i -> i.getDurationMs() != null && i.getDurationMs() >= 5000 && i.getDurationMs() < 10000)
                .count());
        distribution.put("10s+", instances.stream()
                .filter(i -> i.getDurationMs() != null && i.getDurationMs() >= 10000)
                .count());

        return distribution;
    }

    // Get retry statistics

    @Transactional(readOnly = true)
    public Map<String, Object> getRetryStatistics(Long jobId) {
        var instances = instanceRepository
                .findByJob_Id(jobId, Pageable.unpaged())
                .getContent();

        Map<String, Object> stats = new HashMap<>();
        long instancesWithRetries = instances.stream()
                .filter(i -> i.getRetryCount() != null && i.getRetryCount() > 0)
                .count();

        int totalRetries = instances.stream()
                .mapToInt(i -> i.getRetryCount() != null ? i.getRetryCount() : 0)
                .sum();

        stats.put("instancesWithRetries", instancesWithRetries);
        stats.put("totalRetries", totalRetries);

        return stats;
    }

    // Compare multiple jobs

    @Transactional(readOnly = true)
    public List<JobStatisticsDTO> compareJobs(List<Long> jobIds) {
        return jobIds.stream()
                .map(this::calculateJobStatistics)
                .collect(Collectors.toList());
    }

    // Get time range statistics

    @Transactional(readOnly = true)
    public Map<String, Object> getTimeRangeStatistics(LocalDateTime start, LocalDateTime end, Long jobId) {
        var instances = instanceRepository.findByScheduledTimeBetween(start, end);

        if (jobId != null) {
            instances = instances.stream()
                    .filter(i -> i.getJob().getId().equals(jobId))
                    .collect(Collectors.toList());
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalExecutions", instances.size());
        stats.put("successfulExecutions", instances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.COMPLETED)
                .count());
        stats.put("failedExecutions", instances.stream()
                .filter(i -> i.getStatus() == JobInstance.InstanceStatus.FAILED)
                .count());

        return stats;
    }

    //Export statistics as CSV

    @Transactional(readOnly = true)
    public String exportStatisticsAsCSV(Long jobId) {
        JobStatisticsDTO stats = calculateJobStatistics(jobId);

        StringBuilder csv = new StringBuilder();
        csv.append("Metric,Value\n");
        csv.append("Job ID,").append(stats.getJobId()).append("\n");
        csv.append("Job Name,").append(stats.getJobName()).append("\n");
        csv.append("Total Runs,").append(stats.getTotalRuns()).append("\n");
        csv.append("Successful Runs,").append(stats.getSuccessfulRuns()).append("\n");
        csv.append("Failed Runs,").append(stats.getFailedRuns()).append("\n");
        csv.append("Success Rate,").append(String.format("%.2f%%", stats.getSuccessRate())).append("\n");
        csv.append("Average Duration,").append(stats.getAvgDurationFormatted()).append("\n");

        return csv.toString();
    }

    // Get job metrics

    @Transactional(readOnly = true)
    public Map<String, Object> getJobMetrics(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        Map<String, Object> metrics = new HashMap<>();

        long pending = instanceRepository.countByJob_IdAndStatus(
                jobId, JobInstance.InstanceStatus.PENDING);
        long running = instanceRepository.countByJob_IdAndStatus(
                jobId, JobInstance.InstanceStatus.RUNNING);
        long success = instanceRepository.countByJob_IdAndStatus(
                jobId, JobInstance.InstanceStatus.COMPLETED);
        long failed = instanceRepository.countByJob_IdAndStatus(
                jobId, JobInstance.InstanceStatus.FAILED);

        metrics.put("jobId", jobId);
        metrics.put("jobName", job.getName());
        metrics.put("status", job.getStatus());
        metrics.put("pendingInstances", pending);
        metrics.put("runningInstances", running);
        metrics.put("successfulInstances", success);
        metrics.put("failedInstances", failed);
        metrics.put("totalInstances", pending + running + success + failed);

        return metrics;
    }

    // Get job history

    @Transactional(readOnly = true)
    public Map<String, Object> getJobHistory(Long jobId, int page, int size) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("scheduledTime").descending());

        Page<JobInstance> instances = instanceRepository.findByJob_Id(jobId, pageable);

        Map<String, Object> history = new HashMap<>();
        history.put("jobId", jobId);
        history.put("jobName", job.getName());
        history.put("instances", instances.getContent().stream()
                .map(this::mapInstanceToHistory)
                .collect(Collectors.toList()));
        history.put("page", page);
        history.put("size", size);
        history.put("totalElements", instances.getTotalElements());
        history.put("totalPages", instances.getTotalPages());

        return history;
    }

    private Map<String, Object> mapInstanceToHistory(JobInstance instance) {
        Map<String, Object> historyItem = new HashMap<>();
        historyItem.put("id", instance.getId());
        historyItem.put("status", instance.getStatus());
        historyItem.put("scheduledTime", instance.getScheduledTime());
        historyItem.put("startedAt", instance.getStartedAt());
        historyItem.put("completedAt", instance.getCompletedAt());
        historyItem.put("durationMs", instance.getDurationMs());
        historyItem.put("httpStatusCode", instance.getHttpStatusCode());
        historyItem.put("errorMessage", instance.getErrorMessage());
        historyItem.put("retryCount", instance.getRetryCount());
        return historyItem;
    }

    private double calculateHealthScore(JobStatisticsDTO stats) {
        double score = 100.0;

        if (stats.getSuccessRate() < 100) {
            score -= (100 - stats.getSuccessRate()) * 0.5;
        }

        if (stats.getConsecutiveFailures() != null && stats.getConsecutiveFailures() > 0) {
            score -= stats.getConsecutiveFailures() * 10;
        }

        return Math.max(0, score);
    }

    private String getHealthStatus(double score) {
        if (score >= 90) return "EXCELLENT";
        if (score >= 75) return "GOOD";
        if (score >= 50) return "FAIR";
        return "POOR";
    }
}