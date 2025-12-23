package com.example.chronos;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.service.JobMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobMapperTest {

    private final JobMapper mapper = new JobMapper();

    @Test
    void toEntity_mapsCreateRequestCorrectly() {
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

        Job job = mapper.toEntity(request, "ajay");

        assertThat(job.getName()).isEqualTo("Test Job");
        assertThat(job.getTargetUrl()).isEqualTo("https://example.com");
        assertThat(job.getHttpMethod()).isEqualTo(HttpMethodType.GET);
        assertThat(job.getCreatedBy()).isEqualTo("ajay");
        assertThat(job.getCronExpression()).isEqualTo("0 * * * *");
        assertThat(job.getPriority()).isEqualTo(5);
        assertThat(job.getStatus()).isEqualTo(JobStatus.SCHEDULED);
    }

    @Test
    void toResponse_mapsEntityCorrectly() {
        // Use the entity's builder to ensure @PrePersist is called
        Job job = Job.builder()
                .id(1L)
                .name("Test Job")
                .targetUrl("https://example.com")
                .httpMethod(HttpMethodType.GET)
                .status(JobStatus.SCHEDULED)
                .priority(5)
                .timeoutSeconds(30)
                .maxRetries(3)
                .cronExpression("0 * * * *")
                .build();

        // Manually trigger @PrePersist behavior since we're not using JPA
        job.onCreate();

        JobResponse response = mapper.toResponse(job);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Job");
        assertThat(response.getTargetUrl()).isEqualTo("https://example.com");
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }
}