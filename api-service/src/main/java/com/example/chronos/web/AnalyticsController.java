package com.example.chronos.web; // Check if this should be .controller or .web package

import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.repository.JobRepository;
import com.example.chronos.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@RestController
// ✅ FIX 1: Match the Postman URL path
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final JobRepository jobRepository;

    public AnalyticsController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    // ✅ FIX 2: Add the 'dashboard' endpoint Postman expects
    @GetMapping("/dashboard")
    public Map<String, Object> getDashboardStats() {
        var jobs = jobRepository.findAll();

        Map<JobStatus, Long> statusCounts = new EnumMap<>(JobStatus.class);
        for (JobStatus st : JobStatus.values()) {
            long count = jobs.stream().filter(j -> j.getStatus() == st).count();
            statusCounts.put(st, count);
        }

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalJobs", jobs.size());
        dashboard.put("statusCounts", statusCounts);
        return dashboard;
    }

    // ✅ FIX 3: Add the 'job stats' endpoint Postman expects
    @GetMapping("/jobs/{id}/stats")
    public Map<String, Object> getJobStats(@PathVariable Long id) {
        // Simple mock stats since you don't have execution history yet
        if (!jobRepository.existsById(id)) {
            throw new ResourceNotFoundException("Job not found");
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("jobId", id);
        stats.put("totalExecutions", 0); // Placeholder until you add JobExecution
        stats.put("successRate", "0%");
        return stats;
    }
}