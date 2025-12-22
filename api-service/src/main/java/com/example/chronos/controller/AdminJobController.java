package com.example.chronos.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.service.JobService;
import java.util.List;

@RestController
@RequestMapping("/api/admin/jobs")
@Slf4j
public class AdminJobController {

    private final JobService jobService;

    public AdminJobController(JobService jobService) {
        this.jobService = jobService;
    }

    // 1. GET ALL JOBS
    @GetMapping
    public List<JobResponse> listAllJobs() {
        log.info("Admin: Fetching all jobs from database");
        return jobService.listAllJobs();
    }

    // 2. GET JOB BY ID
    @GetMapping("/{id}")
    public JobResponse getJobById(@PathVariable Long id) {
        log.info("Admin: Fetching job by ID: {}", id);
        return jobService.getJobById(id);
    }
}


