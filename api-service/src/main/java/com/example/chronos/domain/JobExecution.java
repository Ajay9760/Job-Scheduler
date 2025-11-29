package com.example.chronos.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "job_executions")
public class JobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Job job;

    private Instant startedAt;
    private Instant completedAt;
    private String status; // SUCCESS, FAILED
    private String exitMessage;

    // Constructors
    public JobExecution() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getExitMessage() { return exitMessage; }
    public void setExitMessage(String exitMessage) { this.exitMessage = exitMessage; }
}