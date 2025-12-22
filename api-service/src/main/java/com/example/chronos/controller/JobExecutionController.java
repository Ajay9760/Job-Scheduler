package com.example.chronos.controller;


import com.example.chronos.dto.job.ApiResponse;
import com.example.chronos.dto.job.JobInstanceDTO;
import com.example.chronos.service.JobExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Job Execution", description = "Manual job execution and control")
public class JobExecutionController {

    private final JobExecutionService executionService;

    @Operation(summary = "Trigger job manually", description = "Execute a job immediately regardless of schedule")
    @PostMapping("/{id}/trigger")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<JobInstanceDTO>> triggerJob(@PathVariable Long id) {
        log.info("🔥 Triggering job: {}", id);

        JobInstanceDTO instance = executionService.triggerManualExecution(id);

        log.info("✅ Created instance ID: {} for job: {}", instance.getId(), id);

        return ResponseEntity.ok(ApiResponse.success(instance,
                "Job triggered successfully. Instance ID: " + instance.getId()));
    }

    @Operation(summary = "Resume job", description = "Resume a paused job")
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> resumeJob(@PathVariable Long id) {
        executionService.resumeJob(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Job resumed successfully"));
    }

    @Operation(summary = "Stop running instance", description = "Cancel a currently running job instance")
    @PostMapping("/instances/{instanceId}/stop")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> stopInstance(@PathVariable Long instanceId) {
        executionService.stopInstance(instanceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Instance stopped successfully"));
    }

    @Operation(summary = "Retry failed instance", description = "Manually retry a failed job instance")
    @PostMapping("/instances/{instanceId}/retry")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")  // ✅ Changed
    public ResponseEntity<ApiResponse<JobInstanceDTO>> retryInstance(@PathVariable Long instanceId) {
        JobInstanceDTO newInstance = executionService.retryFailedInstance(instanceId);
        return ResponseEntity.ok(ApiResponse.success(newInstance, "Retry scheduled successfully"));
    }

    @Operation(summary = "Enable job", description = "Enable a disabled job")
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMIN')")  // Keep ADMIN only
    public ResponseEntity<ApiResponse<Void>> enableJob(@PathVariable Long id) {
        executionService.enableJob(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Job enabled successfully"));
    }

    @Operation(summary = "Disable job", description = "Disable a job (stops all future executions)")
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMIN')")  // Keep ADMIN only
    public ResponseEntity<ApiResponse<Void>> disableJob(@PathVariable Long id) {
        executionService.disableJob(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Job disabled successfully"));
    }

    @Operation(summary = "Bulk trigger jobs", description = "Trigger multiple jobs at once")
    @PostMapping("/bulk-trigger")
    @PreAuthorize("hasRole('ADMIN')")  // Keep ADMIN only
    public ResponseEntity<ApiResponse<Integer>> bulkTriggerJobs(@RequestBody BulkTriggerRequest request) {
        int triggered = executionService.bulkTrigger(request.getJobIds());
        return ResponseEntity.ok(ApiResponse.success(triggered, triggered + " jobs triggered"));
    }

    @lombok.Data
    public static class BulkTriggerRequest {
        private java.util.List<Long> jobIds;
    }
}
