package com.example.chronos.repository;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    // Find jobs by status
    List<Job> findByStatus(JobStatus status);

    // Find jobs by status with pagination
    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    // Find jobs by createdBy (owner - YOUR EXISTING FIELD NAME)
    List<Job> findByCreatedBy(String createdBy);

    // Find job by ID and createdBy (for authorization - YOUR EXISTING FIELD NAME)
    Optional<Job> findByIdAndCreatedBy(Long id, String createdBy);

    // Find active jobs for scheduling
    List<Job> findByStatusAndNextRunAtBefore(JobStatus status, Instant time);
}
