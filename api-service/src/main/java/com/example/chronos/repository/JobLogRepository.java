package com.example.chronos.repository;

import com.example.chronos.domain.JobLog;
import com.example.chronos.domain.JobLog.LogLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;   // 👈 add this import
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobLogRepository extends
        JpaRepository<JobLog, Long>,
        JpaSpecificationExecutor<JobLog> {     // 👈 add this

    // Find logs by instance ID
    Page<JobLog> findByInstanceId(Long instanceId, Pageable pageable);

    // Find logs by job ID
    Page<JobLog> findByJobId(Long jobId, Pageable pageable);

    // Find logs by instance ID ordered by timestamp
    List<JobLog> findByInstanceIdOrderByCreatedAtAsc(Long instanceId);

    Page<JobLog> findByJob_Id(Long jobId, Pageable pageable);

    // Find logs by job ID and log level
    Page<JobLog> findByJob_IdAndLogLevel(Long jobId, LogLevel logLevel, Pageable pageable);
    // Find logs by log level
    Page<JobLog> findByLogLevel(LogLevel logLevel, Pageable pageable);

    // Find logs by instance ID and log level
    List<JobLog> findByInstanceIdAndLogLevel(Long instanceId, LogLevel logLevel);

    // Find error logs for a job
    @Query("""
           SELECT jl FROM JobLog jl
           WHERE jl.job.id = :jobId
             AND jl.logLevel = 'ERROR'
           ORDER BY jl.createdAt DESC
           """)
    Page<JobLog> findErrorLogsByJobId(@Param("jobId") Long jobId, Pageable pageable);

    // Find recent logs for a job
    @Query("""
           SELECT jl FROM JobLog jl
           WHERE jl.job.id = :jobId
           ORDER BY jl.createdAt DESC
           """)
    Page<JobLog> findRecentLogsByJobId(@Param("jobId") Long jobId, Pageable pageable);

    // Count logs by level for an instance
    long countByInstanceIdAndLogLevel(Long instanceId, LogLevel logLevel);

    // Delete old logs (cleanup)
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    // Find logs with details matching criteria
    @Query(value = "SELECT * FROM job_logs WHERE details @> CAST(:jsonQuery AS jsonb)",
            nativeQuery = true)
    List<JobLog> findByDetailsContaining(@Param("jsonQuery") String jsonQuery);

    // Search logs by message pattern
    @Query("""
           SELECT jl FROM JobLog jl
           WHERE jl.instance.id = :instanceId
             AND LOWER(jl.message) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           """)
    List<JobLog> searchLogsByMessage(@Param("instanceId") Long instanceId,
                                   @Param("searchTerm") String searchTerm);
                                   
    @Query("""
           SELECT jl FROM JobLog jl
           WHERE LOWER(jl.message) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
           ORDER BY jl.createdAt DESC
           """)
    List<JobLog> searchAllLogsByMessage(@Param("searchTerm") String searchTerm);

    // Get log statistics for a job
    @Query("""
           SELECT jl.logLevel, COUNT(jl)
           FROM JobLog jl
           WHERE jl.job.id = :jobId
           GROUP BY jl.logLevel
           """)
    List<Object[]> getLogStatisticsByJobId(@Param("jobId") Long jobId);
}
