package com.example.chronos;

import static org.junit.jupiter.api.Assertions.*;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.JobInstance;
import com.example.chronos.domain.JobInstance.InstanceStatus;
import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.domain.enums.ScheduleType;
import com.example.chronos.repository.JobInstanceRepository;
import com.example.chronos.repository.JobLogRepository;
import com.example.chronos.repository.JobRepository;
import com.example.chronos.service.JobLifecycleService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Proves the concurrency guarantees the architecture relies on, against a REAL PostgreSQL
 * (skipped automatically when Docker is not available).
 */
@SpringBootTest(classes = LifecycleIntegrationTest.Cfg.class)
@Testcontainers(disabledWithoutDocker = true)
class LifecycleIntegrationTest {

    @Container
    static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Configuration
    @EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
    @EntityScan("com.example.chronos.domain")
    @EnableJpaRepositories("com.example.chronos.repository")
    static class Cfg {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        JobLifecycleService lifecycle(JobRepository j, JobInstanceRepository i, JobLogRepository l, Clock c) {
            return new JobLifecycleService(j, i, l, c);
        }
    }

    @Autowired JobRepository jobs;
    @Autowired JobInstanceRepository instances;
    @Autowired JobLifecycleService lifecycle;
    @Autowired TransactionTemplate tx;
    @Autowired JdbcTemplate jdbc;

    private final ExecutorService pool = Executors.newFixedThreadPool(16);

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE jobs RESTART IDENTITY CASCADE");
    }

    @AfterAll
    static void noop() { }

    private Job newJob(int maxRetries, ScheduleType type, String cron, Instant nextRun) {
        Job j = new Job();
        j.setName("job");
        j.setTargetUrl("http://example.test/hook");
        j.setHttpMethod(HttpMethodType.GET);
        j.setScheduleType(type);
        j.setCronExpression(cron);
        j.setMaxRetries(maxRetries);
        j.setBackoffSeconds(10);
        j.setCreatedBy("tester");
        j.setNextRunAt(nextRun);
        return jobs.save(j);
    }

    private Instant past(int secondsAgo) {
        return Instant.now().minusSeconds(secondsAgo).truncatedTo(ChronoUnit.MICROS);
    }

    @Test
    void claimIsExactlyOnceUnderContention() throws Exception {
        Job job = newJob(0, ScheduleType.CRON, "* * * * *", null);
        JobInstance inst = lifecycle.createInstance(job, past(5), 0);

        List<Future<Boolean>> results = new ArrayList<>();
        CountDownLatch go = new CountDownLatch(1);
        for (int i = 0; i < 16; i++) {
            String worker = "w" + i;
            results.add(pool.submit(() -> {
                go.await();
                return lifecycle.claim(inst.getId(), worker);
            }));
        }
        go.countDown();
        long winners = 0;
        for (Future<Boolean> f : results) {
            if (f.get()) winners++;
        }
        assertEquals(1, winners, "exactly one worker may win the claim");
    }

    @Test
    void lockDueJobsGivesConcurrentSchedulersDisjointBatches() throws Exception {
        for (int i = 0; i < 100; i++) {
            newJob(0, ScheduleType.CRON, "* * * * *", past(100 - i));
        }
        Instant now = Instant.now();
        CountDownLatch firstHoldsLocks = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<List<Long>> a = pool.submit(() -> tx.execute(s -> {
            List<Long> ids = jobs.lockDueJobs(now, 50).stream().map(Job::getId).toList();
            firstHoldsLocks.countDown();
            await(release);
            return ids;
        }));
        assertTrue(firstHoldsLocks.await(10, TimeUnit.SECONDS));
        List<Long> b = tx.execute(s -> jobs.lockDueJobs(now, 50).stream().map(Job::getId).toList());
        release.countDown();
        List<Long> aIds = a.get();

        assertEquals(50, aIds.size());
        assertEquals(50, b.size());
        assertTrue(Collections.disjoint(aIds, b), "SKIP LOCKED must never hand the same job to two schedulers");
    }

    @Test
    void lockDispatchableGivesConcurrentDispatchersDisjointBatches() throws Exception {
        Job job = newJob(0, ScheduleType.CRON, "* * * * *", null);
        for (int i = 0; i < 100; i++) {
            lifecycle.createInstance(job, past(1000 - i), 0);
        }
        Instant now = Instant.now();
        CountDownLatch firstHoldsLocks = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<Set<Long>> a = pool.submit(() -> tx.execute(s -> {
            Set<Long> ids = ids(instances.lockDispatchable(now, 50));
            firstHoldsLocks.countDown();
            await(release);
            return ids;
        }));
        assertTrue(firstHoldsLocks.await(10, TimeUnit.SECONDS));
        Set<Long> b = tx.execute(s -> ids(instances.lockDispatchable(now, 50)));
        release.countDown();
        Set<Long> aIds = a.get();

        assertEquals(50, aIds.size());
        assertEquals(50, b.size());
        assertTrue(Collections.disjoint(aIds, b));
    }

    @Test
    void concurrentCompletionsDoNotLoseStatUpdates() throws Exception {
        Job job = newJob(0, ScheduleType.CRON, "* * * * *", null);
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Long id = lifecycle.createInstance(job, past(500 - i), 0).getId();
            assertTrue(lifecycle.claim(id, "w"));
            ids.add(id);
        }
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> fs = new ArrayList<>();
        for (Long id : ids) {
            fs.add(pool.submit(() -> {
                go.await();
                lifecycle.finish(id, InstanceStatus.SUCCESS, 100L, 200, "ok", null);
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : fs) f.get();

        Job reloaded = jobs.findById(job.getId()).orElseThrow();
        assertEquals(50, reloaded.getTotalRuns());
        assertEquals(50, reloaded.getSuccessfulRuns());
        assertEquals(100, reloaded.getAvgDurationMs());
    }

    @Test
    void lateDuplicateResultIsIgnored() {
        Job job = newJob(0, ScheduleType.CRON, "* * * * *", null);
        Long id = lifecycle.createInstance(job, past(5), 0).getId();
        lifecycle.claim(id, "w");
        lifecycle.finish(id, InstanceStatus.SUCCESS, 10L, 200, "ok", null);
        lifecycle.finish(id, InstanceStatus.FAILED, 10L, 500, null, "late");
        assertEquals(1, jobs.findById(job.getId()).orElseThrow().getTotalRuns());
        assertEquals("SUCCESS", jdbc.queryForObject("select status from job_instances where id=?", String.class, id));
    }

    @Test
    void failureSchedulesDurableRetryWithBackoffThenStops() {
        Job job = newJob(1, ScheduleType.CRON, "* * * * *", null);
        Long id = lifecycle.createInstance(job, past(5), 0).getId();
        lifecycle.claim(id, "w");
        lifecycle.finish(id, InstanceStatus.FAILED, 5L, 500, null, "HTTP 500");

        List<Map<String, Object>> rows = jdbc.queryForList(
                "select retry_count, status, scheduled_time from job_instances where job_id=? order by retry_count", job.getId());
        assertEquals(2, rows.size());
        assertEquals("PENDING", rows.get(1).get("status"));
        Instant due = ((java.sql.Timestamp) rows.get(1).get("scheduled_time")).toInstant();
        assertTrue(due.isAfter(Instant.now().plusSeconds(5)), "retry must be in the future (backoff), was " + due);

        // retry exhausted -> no third row
        Long retryId = ((Number) jdbc.queryForObject("select id from job_instances where retry_count=1", Long.class)).longValue();
        lifecycle.claim(retryId, "w");
        lifecycle.finish(retryId, InstanceStatus.FAILED, 5L, 500, null, "HTTP 500");
        assertEquals(2, jdbc.queryForObject("select count(*) from job_instances where job_id=?", Integer.class, job.getId()));
    }

    @Test
    void fireDueJobAdvancesCronAndCompletesOnce() {
        Job cron = newJob(0, ScheduleType.CRON, "*/5 * * * *", past(60));
        Job once = newJob(0, ScheduleType.ONCE, "ONCE", past(60));
        tx.executeWithoutResult(s -> {
            lifecycle.fireDueJob(jobs.findById(cron.getId()).orElseThrow());
            lifecycle.fireDueJob(jobs.findById(once.getId()).orElseThrow());
        });
        Job c = jobs.findById(cron.getId()).orElseThrow();
        Job o = jobs.findById(once.getId()).orElseThrow();
        assertTrue(c.getNextRunAt().isAfter(Instant.now()));
        assertEquals(JobStatus.ACTIVE, c.getStatus());
        assertNull(o.getNextRunAt());
        assertEquals(JobStatus.COMPLETED, o.getStatus());
        assertEquals(2, jdbc.queryForObject("select count(*) from job_instances", Integer.class));
    }

    private static Set<Long> ids(List<Object[]> rows) {
        return rows.stream().map(r -> ((Number) r[0]).longValue()).collect(Collectors.toSet());
    }

    private static void await(CountDownLatch l) {
        try {
            l.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
