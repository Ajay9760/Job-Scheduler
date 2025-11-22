package com.example.chronos.repository;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    @Query("select j from Job j where j.status = com.example.chronos.domain.enums.JobStatus.PENDING and j.nextRunAt <= ?1 order by j.priority desc, j.nextRunAt asc")
    List<Job> findDueJobs(Instant now);
}
