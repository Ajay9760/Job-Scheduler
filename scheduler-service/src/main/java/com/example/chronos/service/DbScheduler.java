package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.repository.JobRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class DbScheduler {

    private static final String LOCK_KEY = "chronos:scheduler:lock";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final String JOB_QUEUE = "chronos.jobs.execute";

    private final JobRepository jobRepository;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;

    public DbScheduler(JobRepository jobRepository, RabbitTemplate rabbitTemplate, StringRedisTemplate redisTemplate) {
        this.jobRepository = jobRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.redisTemplate = redisTemplate;
    }

    private boolean acquireLock() {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, "locked", LOCK_TTL);
        return Boolean.TRUE.equals(ok);
    }

    @Scheduled(fixedDelay = 1000)
    public void schedule() {
        if (!acquireLock()) return;
        Instant now = Instant.now();
        List<Job> due = jobRepository.findDueJobs(now);
        for (Job j : due) {
            j.setStatus(JobStatus.SCHEDULED);
            jobRepository.save(j);
            rabbitTemplate.convertAndSend(JOB_QUEUE, j.getId());
        }
    }
}
