package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.dto.job.JobUpdateRequest;
import com.example.chronos.exception.ResourceNotFoundException;
import com.example.chronos.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;

    @Transactional
    public JobResponse create(JobCreateRequest request, String owner) {
        log.info("Creating new job: {} for owner: {}", request.getName(), owner);

        Job job = jobMapper.toEntity(request, owner);

        job.setTotalRuns(0);
        job.setSuccessfulRuns(0);
        job.setFailedRuns(0);
        job.setAvgDurationMs(0L);
        job.setConsecutiveFailures(0);

        job = jobRepository.save(job);
        log.info("Job created with ID: {}", job.getId());

        return jobMapper.toDto(job);
    }

    //List all jobs for a specific user

    @Transactional(readOnly = true)
    public List<JobResponse> listForUser(String owner) {
        List<Job> jobs = jobRepository.findByCreatedBy(owner); // Use your existing field name
        return jobs.stream()
                .map(jobMapper::toDto)
                .collect(Collectors.toList());
    }


   // List ALL jobs in the database (regardless of owner)

    @Transactional(readOnly = true)
    public List<JobResponse> listAllJobs() {
        log.info("Fetching all jobs from database");
        List<Job> jobs = jobRepository.findAll(); // Gets ALL jobs
        log.info("Found {} total jobs in database", jobs.size());
        return jobs.stream()
                .map(jobMapper::toDto)
                .collect(Collectors.toList());
    }
    // Get a specific job for a user

    @Transactional(readOnly = true)
    public JobResponse getForUser(Long id, String owner) {
        Job job = jobRepository.findByIdAndCreatedBy(id, owner) // Use your existing field name
                .orElseThrow(() -> new IllegalArgumentException("Job not found or access denied: " + id));

        log.info("Current user: {}", owner);
        log.info("Job owner (createdBy): {}", job.getCreatedBy());
        if (!job.getCreatedBy().equals(owner)) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied");
        }
        log.info(" Retrieved job {} for user {}", id, owner);
        return jobMapper.toDto(job);
    }

    @Transactional
    public void delete(Long id, String owner) {
        Job job = jobRepository.findByIdAndCreatedBy(id, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));

        log.info("Current user: {}", owner);
        log.info("Job owner (createdBy): {}", job.getCreatedBy());

        if (!job.getCreatedBy().equals(owner)) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied");
        }

        jobRepository.delete(job);
        log.info("🗑️ Deleted job: {} for owner: {}", id, owner);
    }
    // Update a job
    @Transactional
    public JobResponse update(Long id, JobUpdateRequest request, String owner) {
        Job job = jobRepository.findByIdAndCreatedBy(id, owner) // Use your existing field name
                .orElseThrow(() -> new IllegalArgumentException("Job not found or access denied: " + id));

        log.info("Current user: {}", owner);
        log.info("Job owner (createdBy): {}", job.getCreatedBy());

        if (!job.getCreatedBy().equals(owner)) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied");
        }
        log.info("Updating job: {} for owner: {}", id, owner);

        jobMapper.updateEntity(job, request);

        job = jobRepository.save(job);
        return jobMapper.toDto(job);
    }

    // Delete a job
    // Pause a job
    @Transactional
    public void pause(Long id, String owner) {
        Job job = jobRepository.findByIdAndCreatedBy(id, owner) // Use your existing field name
                .orElseThrow(() -> new IllegalArgumentException("Job not found or access denied: " + id));

        log.info("Current user: {}", owner);
        log.info("Job owner (createdBy): {}", job.getCreatedBy());

        if (!job.getCreatedBy().equals(owner)) {
            throw new org.springframework.security.access.AccessDeniedException("Access Denied");
        }

        job.setStatus(com.example.chronos.domain.enums.JobStatus.PAUSED);
        jobRepository.save(job);
        log.info("Paused job: {} for owner: {}", id, owner);
    }

    // ==================== STANDARD METHODS (For Other Controllers) ====================

    // Create job
    @Transactional
    public JobResponse createJob(JobCreateRequest request) {
        return create(request, "system");
    }

    // Get job by ID
    @Transactional(readOnly = true)
    public JobResponse getJobById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));
        return jobMapper.toDto(job);
    }

    // Get all jobs with pagination

    @Transactional(readOnly = true)
    public Page<JobResponse> getAllJobs(String status, Pageable pageable) {
        Page<Job> jobs;

        if (status != null && !status.isEmpty()) {
            // Convert string status to enum
            com.example.chronos.domain.enums.JobStatus jobStatus =
                    com.example.chronos.domain.enums.JobStatus.valueOf(status.toUpperCase());
            jobs = jobRepository.findByStatus(jobStatus, pageable);
        } else {
            jobs = jobRepository.findAll(pageable);
        }

        return jobs.map(jobMapper::toDto);
    }

    // Get active jobs

    @Transactional(readOnly = true)
    public List<JobResponse> getActiveJobs() {
        return jobRepository.findByStatus(com.example.chronos.domain.enums.JobStatus.SCHEDULED)
                .stream()
                .map(jobMapper::toDto)
                .collect(Collectors.toList());
    }

    //  Update job
    @Transactional
    public JobResponse updateJob(Long id, JobUpdateRequest request) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + id));

        jobMapper.updateEntity(job, request);

        job = jobRepository.save(job);
        return jobMapper.toDto(job);
    }

    // Delete job
    @Transactional
    public void deleteJob(Long id) {
        if (!jobRepository.existsById(id)) {
            throw new IllegalArgumentException("Job not found: " + id);
        }
        jobRepository.deleteById(id);
        log.info("Deleted job: {}", id);
    }
}
