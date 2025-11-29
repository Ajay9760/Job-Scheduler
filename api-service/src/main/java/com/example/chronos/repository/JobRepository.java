package com.example.chronos.repository;

import com.example.chronos.domain.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    Page<Job> findByCreatedBy(Long userId, Pageable pageable);

    List<Job> findByStatus(String status);

    Long countByStatus(String status);

    @Query("SELECT j FROM Job j WHERE j.status = :status AND j.nextExecution <= :now ORDER BY j.priority DESC, j.nextExecution ASC")
    List<Job> findDueJobs(@Param("status") String status, @Param("now") LocalDateTime now);

    @Query("SELECT j.status as status, COUNT(j) as count FROM Job j GROUP BY j.status")
    List<Object[]> countByStatusGrouped();

    @Query("SELECT j, j.failureCount as failures FROM Job j WHERE j.failureCount > 0 ORDER BY j.failureCount DESC")
    List<Object[]> findTopFailingJobs();

    @Query("SELECT j FROM Job j WHERE j.name LIKE %:name%")
    Page<Job> findByNameContaining(@Param("name") String name, Pageable pageable);
}

