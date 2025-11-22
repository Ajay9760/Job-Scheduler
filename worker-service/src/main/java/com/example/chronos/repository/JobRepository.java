package com.example.chronos.repository;

import com.example.chronos.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
}
