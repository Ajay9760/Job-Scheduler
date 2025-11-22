package com.example.chronos.repository;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    Optional<Job> findByIdAndCreatedBy(Long id, String createdBy);

    @Query("select j from Job j where j.status in ?1 and j.nextRunAt <= ?2 order by j.priority desc, j.nextRunAt asc")
    List<Job> findDueJobs(List<JobStatus> statuses, Instant now);
}
