package com.example.chronos.controller;

//import com.chronos.api.dto.ApiResponse;
//import com.chronos.api.dto.JobLogDTO;
//import com.chronos.api.dto.PageResponse;
//import com.chronos.api.entity.JobLog.LogLevel;
//import com.chronos.api.service.JobLogService;
import com.example.chronos.domain.JobLog;
import com.example.chronos.dto.auth.PageResponse;
import com.example.chronos.dto.job.ApiResponse;
import com.example.chronos.dto.job.JobLogDTO;
import com.example.chronos.service.JobLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(name = "Job Logs", description = "Query and manage job execution logs")
public class JobLogController {

    private final JobLogService logService;

    @Operation(summary = "Get logs for instance", description = "Retrieve all logs for a specific job instance")
    @GetMapping("/instance/{instanceId}")
    public ResponseEntity<ApiResponse<List<JobLogDTO>>> getLogsByInstance(
            @PathVariable Long instanceId,
            @Parameter(description = "Filter by log level") @RequestParam(required = false) JobLog.LogLevel level
    ) {
        List<JobLogDTO> logs = logService.findByInstanceId(instanceId, level);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @Operation(summary = "Get logs for job", description = "Retrieve logs for all executions of a job")
    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<PageResponse<JobLogDTO>>> getLogsByJob(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) JobLog.LogLevel level
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<JobLogDTO> logs = logService.findByJobId(jobId, level, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @Operation(summary = "Get error logs", description = "Retrieve only error-level logs")
    @GetMapping("/errors")
    public ResponseEntity<ApiResponse<PageResponse<JobLogDTO>>> getErrorLogs(
            @RequestParam(required = false) Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<JobLogDTO> logs = logService.findErrorLogs(jobId, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @Operation(summary = "Search logs", description = "Search logs by message content")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<JobLogDTO>>> searchLogs(
            @RequestParam(required = false) Long instanceId,
            @RequestParam String query
    ) {
        List<JobLogDTO> logs = instanceId != null 
            ? logService.searchLogs(instanceId, query)
            : logService.searchLogs(query);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @Operation(summary = "Get log statistics", description = "Get log count grouped by level for a job")
    @GetMapping("/job/{jobId}/statistics")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getLogStatistics(@PathVariable Long jobId) {
        Map<String, Long> stats = logService.getStatisticsByJobId(jobId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @Operation(summary = "Get recent logs", description = "Get most recent logs across all jobs")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<PageResponse<JobLogDTO>>> getRecentLogs(
            @RequestParam(defaultValue = "100") int limit
    ) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        PageResponse<JobLogDTO> logs = logService.findRecent(pageable);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @Operation(summary = "Export logs", description = "Export logs for an instance as text")
    @GetMapping("/instance/{instanceId}/export")
    public ResponseEntity<String> exportLogs(@PathVariable Long instanceId) {
        String logText = logService.exportLogsAsText(instanceId);
        return ResponseEntity.ok()
                .header("Content-Type", "text/plain")
                .header("Content-Disposition", "attachment; filename=logs-instance-" + instanceId + ".txt")
                .body(logText);
    }

    @Operation(summary = "Delete old logs", description = "Delete logs older than specified days")
    @DeleteMapping("/cleanup")
    public ResponseEntity<ApiResponse<Integer>> cleanupOldLogs(
            @RequestParam(defaultValue = "90") int olderThanDays
    ) {
        int deleted = logService.cleanupOldLogs(olderThanDays);
        return ResponseEntity.ok(ApiResponse.success(deleted, deleted + " logs deleted"));
    }

    @Operation(summary = "Get log by ID", description = "Retrieve a specific log entry")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobLogDTO>> getLogById(@PathVariable Long id) {
        JobLogDTO log = logService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(log));
    }
}
