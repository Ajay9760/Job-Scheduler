package com.example.chronos.repository;

import com.example.chronos.domain.JobInstance;
import com.example.chronos.domain.JobInstance.InstanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JobInstanceRepository extends JpaRepository<JobInstance, Long> {

    @Query("select ji from JobInstance ji " +
            "where ji.status = com.example.chronos.domain.JobInstance$InstanceStatus.PENDING " +
            "and ji.scheduledTime <= :now")
    List<JobInstance> findPendingInstancesReadyToRun(@Param("now") LocalDateTime now);

    Page<JobInstance> findByJobId(Long jobId, Pageable pageable);

    Page<JobInstance> findByJobIdAndStatus(Long jobId, InstanceStatus status, Pageable pageable);
}

