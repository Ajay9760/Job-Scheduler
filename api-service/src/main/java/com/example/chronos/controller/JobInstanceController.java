package com.example.chronos.controller;

import com.example.chronos.domain.JobInstance;
import com.example.chronos.dto.job.ApiResponse;
import com.example.chronos.dto.job.JobInstanceDTO;
import com.example.chronos.dto.auth.PageResponse;
import com.example.chronos.service.JobInstanceService;
import com.example.chronos.repository.JobInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
@Slf4j
public class JobInstanceController {

    private final JobInstanceService jobInstanceService;
    private final JobInstanceRepository jobInstanceRepository;

    // 1. GET ALL INSTANCES
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<JobInstanceDTO>>> getAllInstances(
            @RequestParam(required = false) JobInstance.InstanceStatus status,
            @RequestParam(required = false) Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "scheduledTime,desc") String sort
    ) {
        log.info("Fetching instances - status: {}, jobId: {}, page: {}, size: {}",
                status, jobId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledTime"));
        PageResponse<JobInstanceDTO> instances = jobInstanceService.findAll(pageable, status, jobId);

        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    // 2. GET INSTANCE BY ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobInstanceDTO>> getInstance(@PathVariable Long id) {
        log.info("Fetching instance: {}", id);
        JobInstanceDTO instance = jobInstanceService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(instance));
    }

   // 3. STOP INSTANCE
    @PostMapping("/{id}/stop")
    public ResponseEntity<ApiResponse<JobInstanceDTO>> stopInstance(@PathVariable Long id) {
        log.info("Stopping instance: {}", id);

        JobInstance instance = jobInstanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + id));

        if (instance.getStatus() != JobInstance.InstanceStatus.RUNNING) {
            throw new IllegalStateException("Instance is not running");
        }

        instance.markAsCompleted(JobInstance.InstanceStatus.CANCELLED, "Manually stopped by user");
        jobInstanceRepository.save(instance);

        log.info("Instance {} stopped successfully", id);

        JobInstanceDTO dto = mapToDTO(instance);
        return ResponseEntity.ok(ApiResponse.success(dto, "Instance stopped successfully"));
    }

    // 4. DELETE INSTANCE
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInstance(@PathVariable Long id) {
        log.info("Deleting instance: {}", id);
        jobInstanceService.deleteInstance(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Instance deleted successfully"));
    }

   // 5. GET INSTANCE STATISTICS
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStatistics(
            @RequestParam(required = false) Long jobId
    ) {
        log.info("Fetching instance statistics for jobId: {}", jobId);
        Map<String, Long> stats = jobInstanceService.countByStatus(jobId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

   // 6. GET RECENT INSTANCES
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<PageResponse<JobInstanceDTO>>> getRecentInstances(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<JobInstanceDTO> instances = jobInstanceService.findRecent(pageable);
        return ResponseEntity.ok(ApiResponse.success(instances));
    }

    // 7. CLEANUP OLD INSTANCES
    @PostMapping("/cleanup")
    public ResponseEntity<ApiResponse<Integer>> cleanupOldInstances(
            @RequestParam(defaultValue = "30") int olderThanDays
    ) {
        log.info("Cleaning up instances older than {} days", olderThanDays);
        int deleted = jobInstanceService.cleanupOldInstances(olderThanDays);
        return ResponseEntity.ok(ApiResponse.success(deleted,
                deleted + " instances cleaned up"));
    }

    private JobInstanceDTO mapToDTO(JobInstance instance) {
        JobInstanceDTO dto = JobInstanceDTO.builder()
                .id(instance.getId())
                .jobId(instance.getJob().getId())
                .jobName(instance.getJob().getName())
                .status(instance.getStatus())
                .scheduledTime(instance.getScheduledTime())
                .startedAt(instance.getStartedAt())
                .completedAt(instance.getCompletedAt())
                .durationMs(instance.getDurationMs())
                .httpStatusCode(instance.getHttpStatusCode())
                .responseBody(instance.getResponseBody())
                .errorMessage(instance.getErrorMessage())
                .retryCount(instance.getRetryCount())
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .build();

        dto.computeDerivedFields();
        return dto;
    }
}