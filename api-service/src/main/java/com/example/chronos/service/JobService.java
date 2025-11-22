package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.dto.job.JobUpdateRequest;
import com.example.chronos.exception.ForbiddenException;
import com.example.chronos.exception.ResourceNotFoundException;
import com.example.chronos.repository.JobRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class JobService {

    public static final String JOB_QUEUE = "chronos.jobs.execute";

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final RabbitTemplate rabbitTemplate;

    public JobService(JobRepository jobRepository, JobMapper jobMapper, RabbitTemplate rabbitTemplate) {
        this.jobRepository = jobRepository;
        this.jobMapper = jobMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public JobResponse create(JobCreateRequest request, String owner) {
        Job job = jobMapper.toEntity(request, owner);
        if (job.getNextRunAt() == null) {
            job.setNextRunAt(Instant.now());
        }
        job.setStatus(JobStatus.PENDING);
        Job saved = jobRepository.save(job);
        return jobMapper.toDto(saved);
    }

    public List<JobResponse> listForUser(String owner) {
        return jobRepository.findByCreatedByOrderByCreatedAtDesc(owner)
                .stream()
                .map(jobMapper::toDto)
                .toList();
    }

    public JobResponse getForUser(Long id, String owner) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (!owner.equals(job.getCreatedBy())) {
            throw new ForbiddenException("Job does not belong to current user");
        }
        return jobMapper.toDto(job);
    }

    @Transactional
    public JobResponse update(Long id, JobUpdateRequest request, String owner) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (!owner.equals(job.getCreatedBy())) {
            throw new ForbiddenException("Job does not belong to current user");
        }
        jobMapper.updateEntity(job, request);
        return jobMapper.toDto(jobRepository.save(job));
    }

    @Transactional
    public void delete(Long id, String owner) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (!owner.equals(job.getCreatedBy())) {
            throw new ForbiddenException("Job does not belong to current user");
        }
        jobRepository.delete(job);
    }

    @Transactional
    public void executeNow(Long id, String owner) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (!owner.equals(job.getCreatedBy())) {
            throw new ForbiddenException("Job does not belong to current user");
        }
        job.setNextRunAt(Instant.now());
        job.setStatus(JobStatus.PENDING);
        jobRepository.save(job);
        rabbitTemplate.convertAndSend(JOB_QUEUE, job.getId());
    }
}
