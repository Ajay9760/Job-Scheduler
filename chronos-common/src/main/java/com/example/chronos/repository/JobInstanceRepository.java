package com.example.chronos.repository;

import com.example.chronos.domain.JobInstance;
import com.example.chronos.domain.JobInstance.InstanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@org.springframework.transaction.annotation.Transactional
public interface JobInstanceRepository extends JpaRepository<JobInstance, Long> {

    // ------------------------------------------------------------------ reads (job fetched eagerly via graph, no N+1)

    @EntityGraph(attributePaths = "job")
    Optional<JobInstance> findWithJobById(Long id);

    @Override
    @EntityGraph(attributePaths = "job")
    Page<JobInstance> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "job")
    Page<JobInstance> findByStatus(InstanceStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "job")
    Page<JobInstance> findByJob_Id(Long jobId, Pageable pageable);

    @EntityGraph(attributePaths = "job")
    Page<JobInstance> findByJob_IdAndStatus(Long jobId, InstanceStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "job")
    Page<JobInstance> findByJob_CreatedBy(String createdBy, Pageable pageable);

    @EntityGraph(attributePaths = "job")
    Page<JobInstance> findByJob_CreatedByAndStatus(String createdBy, InstanceStatus status, Pageable pageable);

    long countByJob_IdAndStatusIn(Long jobId, Collection<InstanceStatus> statuses);

    // ------------------------------------------------------------------ outbox / dispatch

    /**
     * Picks PENDING instances that are due, highest job priority first. Rows are locked with
     * SKIP LOCKED so concurrent dispatchers never publish the same instance twice.
     * Returns rows of {@code [instance_id, job_id]}.
     */
    @Query(value = """
            SELECT i.id, i.job_id
            FROM job_instances i
            JOIN jobs j ON j.id = i.job_id
            WHERE i.status = 'PENDING' AND i.scheduled_time <= :now
            ORDER BY j.priority DESC, i.scheduled_time ASC
            LIMIT :limit
            FOR UPDATE OF i SKIP LOCKED
            """, nativeQuery = true)
    List<Object[]> lockDispatchable(@Param("now") Instant now, @Param("limit") int limit);

    @Modifying
    @Query("""
            UPDATE JobInstance i
               SET i.status = com.example.chronos.domain.JobInstance$InstanceStatus.QUEUED,
                   i.dispatchedAt = :now,
                   i.updatedAt = :now
             WHERE i.id IN :ids
               AND i.status = com.example.chronos.domain.JobInstance$InstanceStatus.PENDING
            """)
    int markQueued(@Param("ids") Collection<Long> ids, @Param("now") Instant now);

    /**
     * A QUEUED instance whose message was lost (broker purge, consumer rejected it, DB blip in the worker)
     * goes back to PENDING and is published again. Safe: workers claim with a conditional update.
     */
    @Modifying
    @Query("""
            UPDATE JobInstance i
               SET i.status = com.example.chronos.domain.JobInstance$InstanceStatus.PENDING,
                   i.dispatchedAt = NULL,
                   i.updatedAt = :now
             WHERE i.status = com.example.chronos.domain.JobInstance$InstanceStatus.QUEUED
               AND i.dispatchedAt < :cutoff
            """)
    int requeueStaleQueued(@Param("cutoff") Instant cutoff, @Param("now") Instant now);

    // ------------------------------------------------------------------ worker state transitions (all conditional)

    @Modifying
    @Query("""
            UPDATE JobInstance i
               SET i.status = com.example.chronos.domain.JobInstance$InstanceStatus.RUNNING,
                   i.startedAt = :now,
                   i.workerId = :workerId,
                   i.updatedAt = :now
             WHERE i.id = :id
               AND i.status IN (com.example.chronos.domain.JobInstance$InstanceStatus.PENDING,
                                com.example.chronos.domain.JobInstance$InstanceStatus.QUEUED)
            """)
    int claim(@Param("id") Long id, @Param("workerId") String workerId, @Param("now") Instant now);

    /** RUNNING -> terminal. Returns 0 if the instance is no longer RUNNING (cancelled / timed out meanwhile). */
    @Modifying
    @Query("""
            UPDATE JobInstance i
               SET i.status = :to,
                   i.completedAt = :now,
                   i.durationMs = :durationMs,
                   i.httpStatusCode = :httpStatus,
                   i.responseBody = :body,
                   i.errorMessage = :error,
                   i.updatedAt = :now
             WHERE i.id = :id
               AND i.status = com.example.chronos.domain.JobInstance$InstanceStatus.RUNNING
            """)
    int completeRunning(@Param("id") Long id,
                        @Param("to") InstanceStatus to,
                        @Param("now") Instant now,
                        @Param("durationMs") Long durationMs,
                        @Param("httpStatus") Integer httpStatus,
                        @Param("body") String body,
                        @Param("error") String error);

    /** RUNNING -> PENDING at a later time without consuming a retry (used when a circuit breaker is open). */
    @Modifying
    @Query("""
            UPDATE JobInstance i
               SET i.status = com.example.chronos.domain.JobInstance$InstanceStatus.PENDING,
                   i.scheduledTime = :until,
                   i.dispatchedAt = NULL,
                   i.startedAt = NULL,
                   i.workerId = NULL,
                   i.updatedAt = :now
             WHERE i.id = :id
               AND i.status = com.example.chronos.domain.JobInstance$InstanceStatus.RUNNING
            """)
    int deferRunning(@Param("id") Long id, @Param("until") Instant until, @Param("now") Instant now);

    /** User-requested cancel of anything not finished. A running HTTP call cannot be aborted, only its result ignored. */
    @Modifying
    @Query("""
            UPDATE JobInstance i
               SET i.status = com.example.chronos.domain.JobInstance$InstanceStatus.CANCELLED,
                   i.completedAt = :now,
                   i.errorMessage = :reason,
                   i.updatedAt = :now
             WHERE i.id = :id
               AND i.status IN (com.example.chronos.domain.JobInstance$InstanceStatus.PENDING,
                                com.example.chronos.domain.JobInstance$InstanceStatus.QUEUED,
                                com.example.chronos.domain.JobInstance$InstanceStatus.RUNNING)
            """)
    int cancel(@Param("id") Long id, @Param("reason") String reason, @Param("now") Instant now);

    @Modifying
    @Query("""
            UPDATE JobInstance i
               SET i.status = com.example.chronos.domain.JobInstance$InstanceStatus.SKIPPED,
                   i.completedAt = :now,
                   i.errorMessage = :reason,
                   i.updatedAt = :now
             WHERE i.id = :id
               AND i.status = com.example.chronos.domain.JobInstance$InstanceStatus.RUNNING
            """)
    int skipRunning(@Param("id") Long id, @Param("reason") String reason, @Param("now") Instant now);

    // ------------------------------------------------------------------ maintenance

    /** RUNNING longer than the job's own timeout plus grace: the worker died or hung. */
    @Query(value = """
            SELECT i.id
            FROM job_instances i
            JOIN jobs j ON j.id = i.job_id
            WHERE i.status = 'RUNNING'
              AND i.started_at < :now - ((j.timeout_seconds + :graceSeconds) * INTERVAL '1 second')
            ORDER BY i.started_at ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Long> findTimedOutRunning(@Param("now") Instant now,
                                   @Param("graceSeconds") int graceSeconds,
                                   @Param("limit") int limit);

    /** Batched retention delete (job_logs rows go with them via ON DELETE CASCADE). */
    @Modifying
    @Query(value = """
            DELETE FROM job_instances
            WHERE id IN (
                SELECT id FROM job_instances
                WHERE created_at < :cutoff
                  AND status IN ('SUCCESS', 'FAILED', 'TIMEOUT', 'CANCELLED', 'SKIPPED')
                LIMIT :limit
            )
            """, nativeQuery = true)
    int deleteOldTerminal(@Param("cutoff") Instant cutoff, @Param("limit") int limit);

    @org.springframework.data.jpa.repository.Query("SELECT i.status, COUNT(i) FROM JobInstance i WHERE i.job.id = :jobId GROUP BY i.status")
    List<Object[]> countByStatusForJob(@Param("jobId") Long jobId);

    @org.springframework.data.jpa.repository.Query("SELECT i.status, COUNT(i) FROM JobInstance i WHERE i.job.createdBy = :owner GROUP BY i.status")
    List<Object[]> countByStatusForOwner(@Param("owner") String owner);

    @org.springframework.data.jpa.repository.Query("SELECT i.status, COUNT(i) FROM JobInstance i GROUP BY i.status")
    List<Object[]> countByStatusAll();
}
