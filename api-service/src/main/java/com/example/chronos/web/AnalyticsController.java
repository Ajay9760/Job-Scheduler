package com.example.chronos.web;

import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.repository.JobRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.EnumMap;
import java.util.Map;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final JobRepository jobRepository;

    public AnalyticsController(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @GetMapping("/status-counts")
    public Map<JobStatus, Long> statusCounts() {
        var jobs = jobRepository.findAll();
        Map<JobStatus, Long> result = new EnumMap<>(JobStatus.class);
        for (JobStatus st : JobStatus.values()) {
            long count = jobs.stream().filter(j -> j.getStatus() == st).count();
            result.put(st, count);
        }
        return result;
    }
}
