package com.example.chronos.repository;

import com.example.chronos.domain.JobInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobInstanceRepository extends JpaRepository<JobInstance, UUID> {

    Page<JobInstance> findByJobId(Long jobId, Pageable pageable);

    List<JobInstance> findByJobIdAndStatusIn(Long jobId, List<JobInstance.InstanceStatus> statuses);

    @Query("SELECT ji FROM JobInstance ji WHERE ji.jobId = :jobId AND ji.createdAt >= :startDate")
    List<JobInstance> findRecentByJobId(@Param("jobId") Long jobId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(ji) FROM JobInstance ji WHERE ji.jobId = :jobId AND ji.status = :status")
    Long countByJobIdAndStatus(@Param("jobId") Long jobId, @Param("status") JobInstance.InstanceStatus status);

    @Query("SELECT AVG(ji.executionTimeMs) FROM JobInstance ji WHERE ji.jobId = :jobId AND ji.status = 'COMPLETED' AND ji.executionTimeMs IS NOT NULL")
    Double findAvgExecutionTimeByJobId(@Param("jobId") Long jobId);

    @Query("SELECT MIN(ji.executionTimeMs) FROM JobInstance ji WHERE ji.jobId = :jobId AND ji.status = 'COMPLETED' AND ji.executionTimeMs IS NOT NULL")
    Long findMinExecutionTimeByJobId(@Param("jobId") Long jobId);

    @Query("SELECT MAX(ji.executionTimeMs) FROM JobInstance ji WHERE ji.jobId = :jobId AND ji.status = 'COMPLETED' AND ji.executionTimeMs IS NOT NULL")
    Long findMaxExecutionTimeByJobId(@Param("jobId") Long jobId);

    @Query("SELECT COUNT(ji) FROM JobInstance ji WHERE ji.createdAt >= :startDate AND ji.status = :status")
    Long countByCreatedAtAfterAndStatus(@Param("startDate") LocalDateTime startDate, @Param("status") JobInstance.InstanceStatus status);

    @Query("SELECT EXTRACT(HOUR FROM ji.createdAt) as hour, COUNT(ji) as count FROM JobInstance ji WHERE ji.createdAt >= :startDate GROUP BY EXTRACT(HOUR FROM ji.createdAt) ORDER BY count DESC")
    List<Object[]> findBusiestHours(@Param("startDate") LocalDateTime startDate);
}