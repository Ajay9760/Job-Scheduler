package com.example.chronos.repository;

import com.example.chronos.domain.JobLog;
import com.example.chronos.domain.JobLog.LogLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

@org.springframework.transaction.annotation.Transactional
public interface JobLogRepository extends JpaRepository<JobLog, Long> {

    // ---- by instance (caller must already have checked the instance's job belongs to the user)
    Page<JobLog> findByInstanceId(Long instanceId, Pageable pageable);

    Page<JobLog> findByInstanceIdAndLogLevel(Long instanceId, LogLevel level, Pageable pageable);

    @Query("""
            SELECT l FROM JobLog l
            WHERE l.instanceId = :instanceId
              AND LOWER(l.message) LIKE LOWER(CONCAT('%', :query, '%'))
              AND l.createdAt >= :since
            ORDER BY l.createdAt DESC
            """)
    Page<JobLog> searchByInstance(@Param("instanceId") Long instanceId, @Param("query") String query,
                                  @Param("since") Instant since, Pageable pageable);

    // ---- by job
    Page<JobLog> findByJobId(Long jobId, Pageable pageable);

    Page<JobLog> findByJobIdAndLogLevel(Long jobId, LogLevel level, Pageable pageable);

    @Query("SELECT l.logLevel, COUNT(l) FROM JobLog l WHERE l.jobId = :jobId GROUP BY l.logLevel")
    List<Object[]> countByLevelForJob(@Param("jobId") Long jobId);

    // ---- across all jobs: administrators
    Page<JobLog> findByLogLevel(LogLevel level, Pageable pageable);

    @Query("""
            SELECT l FROM JobLog l
            WHERE LOWER(l.message) LIKE LOWER(CONCAT('%', :query, '%'))
              AND l.createdAt >= :since
            ORDER BY l.createdAt DESC
            """)
    Page<JobLog> searchAll(@Param("query") String query, @Param("since") Instant since, Pageable pageable);

    // ---- across a user's jobs
    @Query("""
            SELECT l FROM JobLog l
            WHERE l.jobId IN (SELECT j.id FROM Job j WHERE j.createdBy = :owner)
            """)
    Page<JobLog> findOwned(@Param("owner") String owner, Pageable pageable);

    @Query("""
            SELECT l FROM JobLog l
            WHERE l.logLevel = :level
              AND l.jobId IN (SELECT j.id FROM Job j WHERE j.createdBy = :owner)
            """)
    Page<JobLog> findOwnedByLevel(@Param("owner") String owner, @Param("level") LogLevel level, Pageable pageable);

    @Query("""
            SELECT l FROM JobLog l
            WHERE LOWER(l.message) LIKE LOWER(CONCAT('%', :query, '%'))
              AND l.createdAt >= :since
              AND l.jobId IN (SELECT j.id FROM Job j WHERE j.createdBy = :owner)
            ORDER BY l.createdAt DESC
            """)
    Page<JobLog> searchOwned(@Param("owner") String owner, @Param("query") String query,
                             @Param("since") Instant since, Pageable pageable);

    // ---- retention
    @Modifying
    @Query(value = """
            DELETE FROM job_logs
            WHERE id IN (SELECT id FROM job_logs WHERE created_at < :cutoff LIMIT :limit)
            """, nativeQuery = true)
    int deleteOld(@Param("cutoff") Instant cutoff, @Param("limit") int limit);
}
