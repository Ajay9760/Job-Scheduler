package com.example.chronos.controller;

import com.example.chronos.dto.auth.PageResponse;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.dto.job.JobInstanceDTO;
import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.dto.job.JobUpdateRequest;
import com.example.chronos.service.JobInstanceService;
import com.example.chronos.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final JobInstanceService jobInstanceService;

    public JobController(JobService jobService, 
                        JobInstanceService jobInstanceService) {
        this.jobService = jobService;
        this.jobInstanceService = jobInstanceService;
    }

    private String resolveOwner(String headerUserId) {
        if (headerUserId != null && !headerUserId.isBlank()) {
            return headerUserId;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
            return auth.getName();
        }
        throw new IllegalStateException("No user identifier available");
    }

    // 1. CREATE JOB
    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody JobCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String owner = resolveOwner(userIdHeader);
        JobResponse response = jobService.create(request, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. LIST JOBS
    @GetMapping
    public List<JobResponse> listJobs(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String owner = resolveOwner(userIdHeader);
        return jobService.listForUser(owner);
    }

    // 3. GET JOB BY ID
    @GetMapping("/{id}")
    public JobResponse getJob(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String owner = resolveOwner(userIdHeader);
        return jobService.getForUser(id, owner);
    }

    // 4. UPDATE JOB
    @PutMapping("/{id}")
    public JobResponse updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String owner = resolveOwner(userIdHeader);
        return jobService.update(id, request, owner);
    }

    // 5. DELETE JOB
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJob(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String owner = resolveOwner(userIdHeader);
        jobService.delete(id, owner);
    }

    // 6. PAUSE JOB
    @PostMapping("/{id}/pause")
    public ResponseEntity<Void> pauseJob(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String owner = resolveOwner(userIdHeader);
        jobService.pause(id, owner);
        return ResponseEntity.ok().build();
    }

    // 7. GET JOB INSTANCES
    @GetMapping("/{jobId}/instances")
    public ResponseEntity<PageResponse<JobInstanceDTO>> getJobInstances(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String owner = resolveOwner(userIdHeader);
        jobService.getForUser(jobId, owner);

        PageResponse<JobInstanceDTO> instances = jobInstanceService.getJobInstances(jobId, page, size);
        return ResponseEntity.ok(instances);
    }

    // 8. GET ACTIVE JOBS
    @GetMapping("/active")
    public ResponseEntity<List<JobResponse>> getActiveJobs(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String owner = resolveOwner(userIdHeader);
        List<JobResponse> jobs = jobService.listForUser(owner);
        // Filter for active jobs only
        List<JobResponse> activeJobs = jobs.stream()
                .filter(job -> "ACTIVE".equals(job.getStatus()) || "SCHEDULED".equals(job.getStatus()))
                .toList();
        return ResponseEntity.ok(activeJobs);
    }

    // 9. SEARCH JOBS
    @GetMapping("/search")
    public ResponseEntity<List<JobResponse>> searchJobs(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String owner = resolveOwner(userIdHeader);
        List<JobResponse> jobs = jobService.listForUser(owner);

        if (name != null && !name.isBlank()) {
            jobs = jobs.stream()
                    .filter(job -> job.getName().toLowerCase().contains(name.toLowerCase()))
                    .toList();
        }

        if (status != null && !status.isBlank()) {
            jobs = jobs.stream()
                    .filter(job -> status.equalsIgnoreCase(String.valueOf(job.getStatus())))
                    .toList();
        }

        return ResponseEntity.ok(jobs);
    }

}

