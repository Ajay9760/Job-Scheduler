package com.example.chronos;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.repository.JobRepository;
import com.example.chronos.service.JobMapper;
import com.example.chronos.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JobServiceTest {

    private JobRepository jobRepository;
    private JobMapper jobMapper;
    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobRepository = mock(JobRepository.class);
        jobMapper = new JobMapper();

        // Initialize JobService with the mocked repository
        jobService = new JobService(jobRepository, jobMapper);
    }

    @Test
    void create_persistsJobAndReturnsResponse() {
        // Arrange
        JobCreateRequest request = new JobCreateRequest();
        request.setName("Test Job");
        request.setHttpUrl("https://example.com");
        request.setHttpMethod("GET");
        request.setScheduleType("CRON");
        request.setCronExpression("0 * * * *");
        request.setPriority(5);
        request.setTimeoutSeconds(30);
        request.setMaxRetries(3);
        request.setBackoffStrategy("LINEAR");

        when(jobRepository.save(any(Job.class)))
                .thenAnswer(invocation -> {
                    Job job = invocation.getArgument(0);
                    job.setId(1L);
                    // Set timestamps since @PrePersist won't fire in tests
                    job.setCreatedAt(LocalDateTime.now());
                    job.setUpdatedAt(LocalDateTime.now());
                    return job;
                });

        // Act
        JobResponse response = jobService.create(request, "ajay");

        // Assert
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Job");
        assertThat(response.getCreatedAt()).isNotNull();
        verify(jobRepository, times(1)).save(any(Job.class));
    }

    @Test
    void listForUser_returnsOnlyUserJobs() {
        // Create properly initialized jobs
        Job job1 = Job.builder()
                .id(1L)
                .name("Job 1")
                .createdBy("ajay")
                .targetUrl("https://example.com")
                .httpMethod(HttpMethodType.GET)
                .status(JobStatus.SCHEDULED)
                .priority(5)
                .timeoutSeconds(30)
                .maxRetries(3)
                .build();
        job1.onCreate();

        Job job2 = Job.builder()
                .id(2L)
                .name("Job 2")
                .createdBy("ajay")
                .targetUrl("https://example.com")
                .httpMethod(HttpMethodType.POST)
                .status(JobStatus.SCHEDULED)
                .priority(5)
                .timeoutSeconds(30)
                .maxRetries(3)
                .build();
        job2.onCreate();

        when(jobRepository.findByCreatedBy("ajay"))
                .thenReturn(List.of(job1, job2));

        List<JobResponse> jobs = jobService.listForUser("ajay");

        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).getId()).isEqualTo(1L);
        assertThat(jobs.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void delete_removesJobWhenOwnedByUser() {
        Job job = Job.builder()
                .id(1L)
                .name("Test Job")
                .createdBy("ajay")
                .targetUrl("https://example.com")
                .httpMethod(HttpMethodType.GET)
                .status(JobStatus.SCHEDULED)
                .priority(5)
                .timeoutSeconds(30)
                .maxRetries(3)
                .build();
        job.onCreate();

        when(jobRepository.findByIdAndCreatedBy(1L, "ajay"))
                .thenReturn(Optional.of(job));

        jobService.delete(1L, "ajay");

        verify(jobRepository, times(1)).delete(job);
    }
}