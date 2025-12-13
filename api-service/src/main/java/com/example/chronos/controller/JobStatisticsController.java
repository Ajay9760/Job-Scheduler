package com.example.chronos.controller;


import com.example.chronos.dto.job.ApiResponse;
import com.example.chronos.dto.job.JobStatisticsDTO;
import com.example.chronos.service.JobStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Tag(name = "Job Statistics", description = "Analytics and metrics for jobs and executions")
public class JobStatisticsController {

    private final JobStatisticsService statisticsService;

    @Operation(summary = "Get job statistics", description = "Get comprehensive statistics for a specific job")
    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<JobStatisticsDTO>> getJobStatistics(@PathVariable Long jobId) {
        JobStatisticsDTO stats = statisticsService.getJobStatistics(jobId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(summary = "Get dashboard overview", description = "Get system-wide statistics for dashboard")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        Map<String, Object> stats = statisticsService.getDashboardStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(summary = "Get job health", description = "Get health metrics for a job (success rate, failures, etc.)")
    @GetMapping("/job/{jobId}/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getJobHealth(@PathVariable Long jobId) {
        Map<String, Object> health = statisticsService.getJobHealth(jobId);
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    @Operation(summary = "Get daily statistics", description = "Get execution statistics grouped by day")
    @GetMapping("/job/{jobId}/daily")
    public ResponseEntity<ApiResponse<List<JobStatisticsDTO.DailyStatistic>>> getDailyStatistics(
            @PathVariable Long jobId,
            @Parameter(description = "Number of days to look back") @RequestParam(defaultValue = "30") int days
    ) {
        List<JobStatisticsDTO.DailyStatistic> stats = statisticsService.getDailyStatistics(jobId, days);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(summary = "Get hourly statistics", description = "Get execution statistics for the last 24 hours")
    @GetMapping("/job/{jobId}/hourly")
    public ResponseEntity<ApiResponse<Map<Integer, Long>>> getHourlyStatistics(@PathVariable Long jobId) {
        Map<Integer, Long> stats = statisticsService.getHourlyStatistics(jobId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(summary = "Get execution trends", description = "Get trend data for job executions")
    @GetMapping("/job/{jobId}/trends")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExecutionTrends(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "7") int days
    ) {
        Map<String, Object> trends = statisticsService.getExecutionTrends(jobId, days);
        return ResponseEntity.ok(ApiResponse.success(trends));
    }

    @Operation(summary = "Get performance metrics", description = "Get detailed performance metrics")
    @GetMapping("/job/{jobId}/performance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPerformanceMetrics(@PathVariable Long jobId) {
        Map<String, Object> metrics = statisticsService.getPerformanceMetrics(jobId);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    @Operation(summary = "Get system-wide statistics", description = "Get aggregated statistics across all jobs")
    @GetMapping("/system")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStatistics() {
        Map<String, Object> stats = statisticsService.getSystemStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(summary = "Get top failing jobs", description = "Get jobs with highest failure rates")
    @GetMapping("/top-failures")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopFailingJobs(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<Map<String, Object>> jobs = statisticsService.getTopFailingJobs(limit);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @Operation(summary = "Get slowest jobs", description = "Get jobs with longest average execution time")
    @GetMapping("/slowest-jobs")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSlowestJobs(
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<Map<String, Object>> jobs = statisticsService.getSlowestJobs(limit);
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @Operation(summary = "Get execution time distribution", description = "Get distribution of execution times")
    @GetMapping("/job/{jobId}/duration-distribution")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getDurationDistribution(@PathVariable Long jobId) {
        Map<String, Long> distribution = statisticsService.getDurationDistribution(jobId);
        return ResponseEntity.ok(ApiResponse.success(distribution));
    }

    @Operation(summary = "Get retry statistics", description = "Get statistics about job retries")
    @GetMapping("/job/{jobId}/retries")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRetryStatistics(@PathVariable Long jobId) {
        Map<String, Object> stats = statisticsService.getRetryStatistics(jobId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(summary = "Compare jobs", description = "Compare statistics between multiple jobs")
    @GetMapping("/compare")
    public ResponseEntity<ApiResponse<List<JobStatisticsDTO>>> compareJobs(
            @RequestParam List<Long> jobIds
    ) {
        List<JobStatisticsDTO> comparison = statisticsService.compareJobs(jobIds);
        return ResponseEntity.ok(ApiResponse.success(comparison));
    }

    @Operation(summary = "Get time range statistics", description = "Get statistics for a specific time period")
    @GetMapping("/time-range")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTimeRangeStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long jobId
    ) {
        Map<String, Object> stats = statisticsService.getTimeRangeStatistics(startDate, endDate, jobId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(summary = "Export statistics", description = "Export statistics as CSV")
    @GetMapping("/job/{jobId}/export")
    public ResponseEntity<String> exportStatistics(@PathVariable Long jobId) {
        String csv = statisticsService.exportStatisticsAsCSV(jobId);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=job-" + jobId + "-statistics.csv")
                .body(csv);
    }
}