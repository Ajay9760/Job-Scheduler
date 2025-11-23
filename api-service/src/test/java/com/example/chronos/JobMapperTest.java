package com.example.chronos;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.dto.job.JobUpdateRequest;
import com.example.chronos.service.JobMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JobMapperTest {

    private final JobMapper mapper = new JobMapper();

    @Test
    void toEntityCopiesFieldsAndOwner() {
        JobCreateRequest req = new JobCreateRequest();
        req.setName("My Job");
        req.setTargetUrl("https://example.com");
        req.setHttpMethod(HttpMethodType.POST);
        req.setRequestBody("{\"ok\":true}");
        req.setPriority(5);
        req.setTimeoutSeconds(30);
        req.setMaxRetries(3);
        req.setBackoffSeconds(10);
        req.setWebhookUrl("https://example.com/hook");
        req.setCronExpression("*/5 * * * *");

        String owner = "ajay";

        Job job = mapper.toEntity(req, owner);

        assertThat(job.getName()).isEqualTo("My Job");
        assertThat(job.getTargetUrl()).isEqualTo("https://example.com");
        assertThat(job.getHttpMethod()).isEqualTo(HttpMethodType.POST);
        assertThat(job.getRequestBody()).isEqualTo("{\"ok\":true}");
        assertThat(job.getPriority()).isEqualTo(5);
        assertThat(job.getTimeoutSeconds()).isEqualTo(30);
        assertThat(job.getMaxRetries()).isEqualTo(3);
        assertThat(job.getBackoffSeconds()).isEqualTo(10);
        assertThat(job.getWebhookUrl()).isEqualTo("https://example.com/hook");
        assertThat(job.getCronExpression()).isEqualTo("*/5 * * * *");
        assertThat(job.getCreatedBy()).isEqualTo("ajay");
    }

    @Test
    void updateEntityOnlyOverwritesNonNullFields() {
        Job job = new Job();
        job.setName("Old Name");
        job.setTargetUrl("https://old.com");
        job.setCronExpression("0 * * * *");
        job.setPriority(1);
        job.setTimeoutSeconds(15);
        job.setMaxRetries(2);
        job.setBackoffSeconds(5);
        job.setWebhookUrl("https://old.com/hook");

        JobUpdateRequest update = new JobUpdateRequest();
        update.setName("New Name");
        update.setTargetUrl(null); // should NOT overwrite
        update.setCronExpression("*/10 * * * *");
        update.setPriority(10);
        update.setTimeoutSeconds(null); // should NOT overwrite
        update.setMaxRetries(5);
        update.setBackoffSeconds(null); // should NOT overwrite
        update.setWebhookUrl("https://new.com/hook");

        mapper.updateEntity(job, update);

        assertThat(job.getName()).isEqualTo("New Name");
        assertThat(job.getTargetUrl()).isEqualTo("https://old.com"); // unchanged
        assertThat(job.getCronExpression()).isEqualTo("*/10 * * * *");
        assertThat(job.getPriority()).isEqualTo(10);
        assertThat(job.getTimeoutSeconds()).isEqualTo(15); // unchanged
        assertThat(job.getMaxRetries()).isEqualTo(5);
        assertThat(job.getBackoffSeconds()).isEqualTo(5); // unchanged
        assertThat(job.getWebhookUrl()).isEqualTo("https://new.com/hook");
    }

    @Test
    void toDtoCopiesAllFields() {
        Instant now = Instant.now();

        Job job = new Job();
        job.setId(1L);
        job.setExternalId("ext-123");
        job.setName("Job Name");
        job.setTargetUrl("https://example.com");
        job.setHttpMethod(HttpMethodType.GET);
        job.setCronExpression("0 * * * *");
        job.setStatus(JobStatus.PENDING);
        job.setPriority(3);
        job.setTimeoutSeconds(20);
        job.setMaxRetries(4);
        job.setRetryCount(1);
        job.setBackoffSeconds(7);
        job.setWebhookUrl("https://example.com/hook");
        job.setNextRunAt(now);
        job.setCreatedAt(now.minusSeconds(60));
        job.setUpdatedAt(now);
        job.setLastError("Something failed");

        JobResponse dto = mapper.toDto(job);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getExternalId()).isEqualTo("ext-123");
        assertThat(dto.getName()).isEqualTo("Job Name");
        assertThat(dto.getTargetUrl()).isEqualTo("https://example.com");
        assertThat(dto.getHttpMethod()).isEqualTo(HttpMethodType.GET);
        assertThat(dto.getCronExpression()).isEqualTo("0 * * * *");
        assertThat(dto.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(dto.getPriority()).isEqualTo(3);
        assertThat(dto.getTimeoutSeconds()).isEqualTo(20);
        assertThat(dto.getMaxRetries()).isEqualTo(4);
        assertThat(dto.getRetryCount()).isEqualTo(1);
        assertThat(dto.getBackoffSeconds()).isEqualTo(7);
        assertThat(dto.getWebhookUrl()).isEqualTo("https://example.com/hook");
        assertThat(dto.getNextRunAt()).isEqualTo(now);
        assertThat(dto.getCreatedAt()).isEqualTo(now.minusSeconds(60));
        assertThat(dto.getUpdatedAt()).isEqualTo(now);
        assertThat(dto.getLastError()).isEqualTo("Something failed");
    }
}
