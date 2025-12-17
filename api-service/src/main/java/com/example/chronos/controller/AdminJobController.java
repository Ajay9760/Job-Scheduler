package com.example.chronos.controller;

import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.service.JobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/jobs")
@Slf4j
public class AdminJobController {

    private final JobService jobService;

    public AdminJobController(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * LIST ALL JOBS (Admin endpoint - shows ALL jobs regardless of owner)
     * GET /api/admin/jobs
     */
    @GetMapping
    public List<JobResponse> listAllJobs() {
        log.info("Admin: Fetching all jobs from database");
        return jobService.listAllJobs();
    }

    /**
     * GET ANY JOB BY ID (Admin endpoint - no ownership check)
     * GET /api/admin/jobs/{id}
     */
    @GetMapping("/{id}")
    public JobResponse getJobById(@PathVariable Long id) {
        log.info("Admin: Fetching job by ID: {}", id);
        return jobService.getJobById(id);
    }
}