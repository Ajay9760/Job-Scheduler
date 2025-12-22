package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.dto.job.JobUpdateRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class JobMapper {

    public Job toEntity(JobCreateRequest dto, String owner) {
        Job j = new Job();
        j.setName(dto.getName());
        j.setTargetUrl(dto.getTargetUrl());
        j.setHttpMethod(HttpMethodType.valueOf(dto.getHttpMethod().toUpperCase()));
        j.setRequestBody(dto.getRequestBody());
        j.setPriority(dto.getPriority() != null ? dto.getPriority() : 5);
        j.setTimeoutSeconds(dto.getTimeoutSeconds() != null ? dto.getTimeoutSeconds() : 30);
        j.setMaxRetries(dto.getMaxRetries() != null ? dto.getMaxRetries() : 3);
        j.setBackoffSeconds(dto.getBackoffSeconds() != null ? dto.getBackoffSeconds() : 60L);
        j.setWebhookUrl(dto.getWebhookUrl());
        j.setCronExpression(dto.getCronExpression());
        j.setCreatedBy(owner);
        j.setStatus(JobStatus.SCHEDULED);
        return j;
    }

    public void updateEntity(Job job, JobUpdateRequest dto) {
        if (dto.getName() != null) job.setName(dto.getName());
        if (dto.getTargetUrl() != null) job.setTargetUrl(dto.getTargetUrl());
        if (dto.getHttpMethod() != null) job.setHttpMethod(HttpMethodType.valueOf(dto.getHttpMethod().toUpperCase())); // ✅ Convert String to enum
        if (dto.getCronExpression() != null) job.setCronExpression(dto.getCronExpression());
        if (dto.getPriority() != null) job.setPriority(dto.getPriority());
        if (dto.getTimeoutSeconds() != null) job.setTimeoutSeconds(dto.getTimeoutSeconds());
        if (dto.getMaxRetries() != null) job.setMaxRetries(dto.getMaxRetries());
        if (dto.getBackoffSeconds() != null) job.setBackoffSeconds(dto.getBackoffSeconds());
        if (dto.getWebhookUrl() != null) job.setWebhookUrl(dto.getWebhookUrl());
    }

    public JobResponse toDto(Job job) {
        JobResponse r = new JobResponse();
        r.setId(job.getId());
        r.setExternalId(job.getExternalId());
        r.setName(job.getName());
        r.setTargetUrl(job.getTargetUrl());
        r.setHttpMethod(HttpMethodType.valueOf(job.getHttpMethod().name())); // ✅ Convert enum to String
        r.setCronExpression(job.getCronExpression());
        r.setStatus(job.getStatus());
        r.setPriority(job.getPriority());
        r.setTimeoutSeconds(job.getTimeoutSeconds());
        r.setMaxRetries(job.getMaxRetries());
        r.setRetryCount(job.getRetryCount());
        r.setBackoffSeconds(job.getBackoffSeconds());
        r.setWebhookUrl(job.getWebhookUrl());
        r.setNextRunAt(Instant.from(job.getNextRunAt()));
        r.setCreatedAt(Instant.from(job.getCreatedAt()));
        r.setUpdatedAt(Instant.from(job.getUpdatedAt()));
        r.setLastError(job.getLastError());
        return r;
    }

    public JobResponse toResponse(Job job) {
        return toDto(job);
    }
}