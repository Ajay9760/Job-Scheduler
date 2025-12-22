package com.example.chronos.repository;

import com.example.chronos.domain.JobInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobINstanceRepository extends
        JpaRepository<JobInstance, Long>,
        JpaSpecificationExecutor<JobInstance> {

    // Find instances by job ID
    Page<JobInstance> findByJob_Id(Long jobId, Pageable pageable);

    // Find instances by status
    List<JobInstance> findByStatus(JobInstance.InstanceStatus status);

    // Find instances by job ID and status
    Page<JobInstance> findByJob_IdAndStatus(Long jobId,
                                            JobInstance.InstanceStatus status,
                                            Pageable pageable);

    // Find latest instance for a job
    Optional<JobInstance> findFirstByJob_IdOrderByCreatedAtDesc(Long jobId);

    // Find instances scheduled between dates
    List<JobInstance> findByScheduledTimeBetween(LocalDateTime start, LocalDateTime end);

    // Find instances ready to run (PENDING status, scheduled time passed)
    @Query("""
           SELECT ji FROM JobInstance ji
           WHERE ji.status = 'PENDING'
             AND ji.scheduledTime <= :currentTime
           ORDER BY ji.scheduledTime ASC
           """)
    List<JobInstance> findPendingInstancesReadyToRun(@Param("currentTime") LocalDateTime currentTime);

    // Count instances by job ID and status
    long countByJob_IdAndStatus(Long jobId, JobInstance.InstanceStatus status);

    // Get statistics for a job
    @Query("""
           SELECT COUNT(ji), AVG(ji.durationMs), MAX(ji.durationMs), MIN(ji.durationMs)
           FROM JobInstance ji
           WHERE ji.job.id = :jobId AND ji.status = 'SUCCESS'
           """)
    Object[] getJobStatistics(@Param("jobId") Long jobId);

    // Find failed instances for retry
    @Query("""
       SELECT ji FROM JobInstance ji
       WHERE ji.status = 'FAILED'
         AND ji.retryCount < ji.job.maxRetries
         AND ji.retryCount < :maxRetries
       ORDER BY ji.scheduledTime ASC
       """)
    List<JobInstance> findFailedInstancesForRetry(@Param("maxRetries") int maxRetries);

    // Delete old instances (cleanup)
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    // Find recent instances
    @Query("""
           SELECT ji FROM JobInstance ji
           WHERE ji.job.id = :jobId
           ORDER BY ji.createdAt DESC
           """)
    Page<JobInstance> findRecentInstancesByJobId(@Param("jobId") Long jobId, Pageable pageable);

    // Count total runs for a job
    long countByJob_Id(Long jobId);

    // Find running instances
    @Query("""
           SELECT ji FROM JobInstance ji
           WHERE ji.status = 'RUNNING'
             AND ji.startedAt < :timeoutThreshold
           """)
    List<JobInstance> findStuckRunningInstances(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);
}