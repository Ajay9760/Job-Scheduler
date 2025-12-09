package com.example.chronos;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.repository.JobRepository;
import com.example.chronos.service.JobMapper;
import com.example.chronos.service.JobService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JobServiceTest {

    @Test
    void createPersistsJobWithOwner() {
        // Arrange
        JobRepository repo = mock(JobRepository.class);
        JobMapper mapper = new JobMapper();

        // Create service with only 2 arguments
        JobService service = new JobService(repo, mapper);

        JobCreateRequest req = new JobCreateRequest();
        req.setName("Test Job");
        req.setTargetUrl("https://example.com");
        req.setHttpMethod(String.valueOf(HttpMethodType.GET));
        req.setCronExpression("0 0 * * * *"); // Required field

        when(repo.save(any(Job.class))).thenAnswer(invocation -> {
            Job j = invocation.getArgument(0);
            j.setId(1L);
            return j;
        });

        // Act
        var resp = service.create(req, "ajay");

        // Assert
        assertThat(resp.getId()).isEqualTo(1L);
        assertThat(resp.getName()).isEqualTo("Test Job");
        assertThat(resp.getTargetUrl()).isEqualTo("https://example.com");

        // Verify save was called once
        verify(repo, times(1)).save(any(Job.class));
    }

    @Test
    void listForUserReturnsOnlyUserJobs() {
        // Arrange
        JobRepository repo = mock(JobRepository.class);
        JobMapper mapper = new JobMapper();
        JobService service = new JobService(repo, mapper);

        Job job1 = new Job();
        job1.setId(1L);
        job1.setName("Job 1");
        job1.setCreatedBy("ajay");

        Job job2 = new Job();
        job2.setId(2L);
        job2.setName("Job 2");
        job2.setCreatedBy("ajay");

        when(repo.findByCreatedBy("ajay")).thenReturn(java.util.List.of(job1, job2));

        // Act
        var jobs = service.listForUser("ajay");

        // Assert
        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).getId()).isEqualTo(1L);
        assertThat(jobs.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void deleteRemovesJob() {
        // Arrange
        JobRepository repo = mock(JobRepository.class);
        JobMapper mapper = new JobMapper();
        JobService service = new JobService(repo, mapper);

        Job job = new Job();
        job.setId(1L);
        job.setCreatedBy("ajay");

        when(repo.findByIdAndCreatedBy(1L, "ajay")).thenReturn(java.util.Optional.of(job));

        // Act
        service.delete(1L, "ajay");

        // Assert
        verify(repo, times(1)).delete(job);
    }
}