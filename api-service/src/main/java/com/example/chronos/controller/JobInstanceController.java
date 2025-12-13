package com.example.chronos.controller;


import com.example.chronos.domain.JobInstance;
import com.example.chronos.dto.auth.PageResponse;
import com.example.chronos.dto.job.ApiResponse;
import com.example.chronos.dto.job.JobInstanceDTO;
import com.example.chronos.service.JobInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
@Tag(name = "Job Instances", description = "Query and manage job execution instances")
public class JobInstanceController {

    private final JobInstanceService instanceService;

    @Operation(summary = "Get all instances", description = "Retrieve all job instances with pagination and filtering")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<JobInstanceDTO>>> getAllInstances(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "DESC") String sortDir,
            @Parameter(description = "Filter by status") @RequestParam(required = false) JobInstance.InstanceStatus status,
            @Parameter(description = "Filter by job ID") @RequestParam(required = false) Long jobId
    ) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.Direction.fromString(sortDir), sortBy);

        PageResponse<JobInstanceDTO> instances = instanceService.findAll(pageable, status, jobId);
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    @Operation(summary = "Get instance by ID", description = "Retrieve detailed information about a specific instance")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobInstanceDTO>> getInstanceById(@PathVariable Long id) {
        JobInstanceDTO instance = instanceService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(instance));
    }

    @Operation(summary = "Get instances for a job", description = "Retrieve all execution instances for a specific job")
    @GetMapping("/job/{jobId}")
    public ResponseEntity<ApiResponse<PageResponse<JobInstanceDTO>>> getInstancesByJob(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) JobInstance.InstanceStatus status
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<JobInstanceDTO> instances = instanceService.findByJobId(jobId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    @Operation(summary = "Get recent instances", description = "Get the most recent job instances")
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<PageResponse<JobInstanceDTO>>> getRecentInstances(
            @RequestParam(defaultValue = "10") int limit
    ) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        PageResponse<JobInstanceDTO> instances = instanceService.findRecent(pageable);
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    @Operation(summary = "Get running instances", description = "Get all currently running job instances")
    @GetMapping("/running")
    public ResponseEntity<ApiResponse<PageResponse<JobInstanceDTO>>> getRunningInstances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startedAt").descending());
        PageResponse<JobInstanceDTO> instances = instanceService.findByStatus(JobInstance.InstanceStatus.RUNNING, pageable);
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    @Operation(summary = "Get failed instances", description = "Get all failed job instances")
    @GetMapping("/failed")
    public ResponseEntity<ApiResponse<PageResponse<JobInstanceDTO>>> getFailedInstances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("completedAt").descending());
        PageResponse<JobInstanceDTO> instances = instanceService.findByStatus(JobInstance.InstanceStatus.FAILED, pageable);
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    @Operation(summary = "Get instances by date range", description = "Retrieve instances within a specific time range")
    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<PageResponse<JobInstanceDTO>>> getInstancesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("scheduledTime").descending());
        PageResponse<JobInstanceDTO> instances = instanceService.findByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    @Operation(summary = "Get latest instance for job", description = "Get the most recent execution instance for a job")
    @GetMapping("/job/{jobId}/latest")
    public ResponseEntity<ApiResponse<JobInstanceDTO>> getLatestInstanceForJob(@PathVariable Long jobId) {
        JobInstanceDTO instance = instanceService.findLatestByJobId(jobId);
        return ResponseEntity.ok(ApiResponse.success(instance));
    }

    @Operation(summary = "Count instances by status", description = "Get count of instances grouped by status")
    @GetMapping("/count-by-status")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> countByStatus(
            @RequestParam(required = false) Long jobId
    ) {
        var counts = instanceService.countByStatus(jobId);
        return ResponseEntity.ok(ApiResponse.success(counts));
    }

    @Operation(summary = "Delete instance", description = "Delete a specific job instance")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInstance(@PathVariable Long id) {
        instanceService.deleteInstance(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Instance deleted successfully"));
    }

    @Operation(summary = "Cleanup old instances", description = "Delete instances older than specified days")
    @DeleteMapping("/cleanup")
    public ResponseEntity<ApiResponse<Integer>> cleanupOldInstances(
            @RequestParam(defaultValue = "30") int olderThanDays
    ) {
        int deleted = instanceService.cleanupOldInstances(olderThanDays);
        return ResponseEntity.ok(ApiResponse.success(deleted, deleted + " instances deleted"));
    }
}
