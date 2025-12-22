package com.example.chronos.controller;

import com.example.chronos.dto.job.ApiResponse;
import com.example.chronos.service.SchedulerManagementService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final SchedulerManagementService schedulerService;

    @Operation(summary = "Get scheduler status", description = "Get scheduler status")
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSchedulerStatus() {
        Map<String, Object> status = schedulerService.getStatus();
        return ResponseEntity.ok(ApiResponse.success(status, "Scheduler status retrieved"));
    }

    @Operation(summary = "Force schedule all jobs", description = "Force schedule all jobs")
    @PostMapping("/force-schedule")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> forceSchedule() {
        int scheduled = schedulerService.forceScheduleAllJobs();
        return ResponseEntity.ok(ApiResponse.success(
                scheduled,
                scheduled + " jobs scheduled"
        ));
    }
    @Operation(summary = "Calculate next runs", description = "Calculate next runs")
    @PostMapping("/calculate-next-runs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Integer>> calculateNextRuns() {
        int updated = schedulerService.calculateAllNextRuns();
        return ResponseEntity.ok(ApiResponse.success(
                updated,
                updated + " jobs updated"
        ));
    }

    @Operation(summary = "Cleanup", description = "Cleanup")
    @PostMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> cleanup(
            @RequestParam(defaultValue = "30") int olderThanDays) {

        Map<String, Integer> result = schedulerService.cleanup(olderThanDays);
        return ResponseEntity.ok(ApiResponse.success(result, "Cleanup completed"));
    }
}