package com.example.chronos;

import com.example.chronos.domain.Job;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.repository.JobRepository;
import com.example.chronos.service.JobInstanceService;
import com.example.chronos.service.JobMapper;
import com.example.chronos.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JobServiceTest {

    private JobRepository jobRepository;
    private JobInstanceService jobInstanceService;
    private RabbitTemplate rabbitTemplate;

    private JobMapper jobMapper;
    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobRepository = mock(JobRepository.class);
        jobInstanceService = mock(JobInstanceService.class);
        rabbitTemplate = mock(RabbitTemplate.class);

        jobMapper = new JobMapper();
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
                    return job;
                });

        // Act
        JobResponse response = jobService.create(request, "ajay");

        // Assert
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Job");
        verify(jobRepository, times(1)).save(any(Job.class));
        verifyNoInteractions(jobInstanceService,rabbitTemplate);
    }

    @Test
    void listForUser_returnsOnlyUserJobs() {
        Job job1 = new Job();
        job1.setId(1L);
        job1.setName("Job 1");
        job1.setCreatedBy("ajay");

        Job job2 = new Job();
        job2.setId(2L);
        job2.setName("Job 2");
        job2.setCreatedBy("ajay");

        when(jobRepository.findByCreatedBy("ajay"))
                .thenReturn(List.of(job1, job2));

        List<JobResponse> jobs = jobService.listForUser("ajay");

        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).getId()).isEqualTo(1L);
        assertThat(jobs.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void delete_removesJobWhenOwnedByUser() {
        Job job = new Job();
        job.setId(1L);
        job.setCreatedBy("ajay");

        when(jobRepository.findByIdAndCreatedBy(1L, "ajay"))
                .thenReturn(Optional.of(job));

        jobService.delete(1L, "ajay");

        verify(jobRepository, times(1)).delete(job);
    }
}
