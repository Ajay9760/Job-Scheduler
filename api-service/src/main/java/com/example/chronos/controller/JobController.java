package com.example.chronos.controller;

import com.example.chronos.domain.JobExecution;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.dto.job.JobUpdateRequest;
import com.example.chronos.repository.JobExecutionRepository; // ✅ Import Repository
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
    private final JobExecutionRepository jobExecutionRepository; // ✅ Added Field

    public JobController(JobService jobService, JobExecutionRepository jobExecutionRepository) {
        this.jobService = jobService;
        this.jobExecutionRepository = jobExecutionRepository;
    }

    /**
     * Resolve the "owner" of the job.
     * - Prefer X-User-Id header if present
     * - Otherwise fall back to the authenticated principal's username
     */
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
}

