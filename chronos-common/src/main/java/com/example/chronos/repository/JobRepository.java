package com.example.chronos.repository;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@org.springframework.transaction.annotation.Transactional
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    Optional<Job> findByIdAndCreatedBy(Long id, String createdBy);

    Page<Job> findByCreatedBy(String createdBy, Pageable pageable);

    Page<Job> findByStatus(JobStatus status, Pageable pageable);

    long countByCreatedBy(String createdBy);

    long countByStatus(JobStatus status);

    /**
     * Claims due jobs for this scheduler instance. {@code FOR UPDATE SKIP LOCKED} means several
     * scheduler replicas can run this concurrently and each gets a disjoint set of rows, so no
     * distributed lock is needed for correctness. Must run inside a transaction.
     */
    @Query(value = """
            SELECT * FROM jobs
            WHERE status = 'ACTIVE' AND next_run_at <= :now
            ORDER BY priority DESC, next_run_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Job> lockDueJobs(@Param("now") Instant now, @Param("limit") int limit);

    /**
     * Counters are updated with a single atomic UPDATE (not read-modify-write) so concurrent workers
     * never lose increments. They deliberately do not touch {@code version}, so a user editing the
     * job is not rejected just because an execution finished.
     * The running average uses the pre-update values of both columns (SQL semantics).
     */
    @Modifying
    @Query(value = """
            UPDATE jobs SET
                total_runs = total_runs + 1,
                successful_runs = successful_runs + 1,
                consecutive_failures = 0,
                avg_duration_ms = ((avg_duration_ms * successful_runs) + :durationMs) / (successful_runs + 1),
                last_run_at = :now,
                last_success_at = :now,
                last_error = NULL
            WHERE id = :id
            """, nativeQuery = true)
    int recordSuccess(@Param("id") Long id, @Param("now") Instant now, @Param("durationMs") long durationMs);

    @Modifying
    @Query(value = """
            UPDATE jobs SET
                total_runs = total_runs + 1,
                failed_runs = failed_runs + 1,
                consecutive_failures = consecutive_failures + 1,
                last_run_at = :now,
                last_failure_at = :now,
                last_error = :error
            WHERE id = :id
            """, nativeQuery = true)
    int recordFailure(@Param("id") Long id, @Param("now") Instant now, @Param("error") String error);

    @org.springframework.data.jpa.repository.Query("SELECT j.status, COUNT(j) FROM Job j WHERE j.createdBy = :owner GROUP BY j.status")
    List<Object[]> countByStatusForOwner(@Param("owner") String owner);

    @org.springframework.data.jpa.repository.Query("SELECT j.status, COUNT(j) FROM Job j GROUP BY j.status")
    List<Object[]> countByStatusAll();
}
