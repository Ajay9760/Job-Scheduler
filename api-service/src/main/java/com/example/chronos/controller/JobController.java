package com.example.chronos.controller;

import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;

    @Autowired
    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @RequestBody JobCreateRequest request,
            @AuthenticationPrincipal(expression = "username") String username) {
        JobResponse response = jobService.create(request, username);
        return ResponseEntity.status(201).body(response);
    }
}
