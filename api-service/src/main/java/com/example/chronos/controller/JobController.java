package com.example.chronos.controller;

import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.dto.job.JobUpdateRequest;
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

    public JobController(JobService jobService) {
        this.jobService = jobService;
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

    // POST /api/jobs  -> used by JobControllerTest + JobControllerValidationTest
    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody JobCreateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String owner = resolveOwner(userIdHeader);
        JobResponse response = jobService.create(request, owner);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/jobs  -> list jobs for current user
    @GetMapping
    public List<JobResponse> listJobs(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String owner = resolveOwner(userIdHeader);
        return jobService.listForUser(owner);
    }

    // GET /api/jobs/{id} -> used by JobControllerNotFoundTest + JobControllerForbiddenTest
    @GetMapping("/{id}")
    public JobResponse getJob(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userIdHeader
    ) {
        // ownership check is inside JobService.getForUser(...)
        return jobService.getForUser(id, userIdHeader);
    }

    // PUT /api/jobs/{id} -> update job
    @PutMapping("/{id}")
    public JobResponse updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobUpdateRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String owner = resolveOwner(userIdHeader);
        return jobService.update(id, request, owner);
    }

    // DELETE /api/jobs/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJob(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String owner = resolveOwner(userIdHeader);
        jobService.delete(id, owner);
    }

    // POST /api/jobs/{id}/execute -> trigger immediate execution
    @PostMapping("/{id}/execute")
    public ResponseEntity<Void> executeNow(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader
    ) {
        String owner = resolveOwner(userIdHeader);
        jobService.executeNow(id, owner);
        return ResponseEntity.accepted().build();
    }
}
