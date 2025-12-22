package com.example.chronos;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.HttpMethodType;
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
    }

    @Test
    void toResponse_mapsEntityCorrectly() {
        Job job = new Job();
        job.setId(1L);
        job.setName("Test Job");
        job.setTargetUrl("https://example.com");
        job.setHttpMethod(HttpMethodType.GET);

        JobResponse response = mapper.toResponse(job);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Test Job");
    }
}
