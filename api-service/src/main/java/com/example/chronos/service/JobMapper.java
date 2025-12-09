package com.example.chronos.service;

import com.example.chronos.domain.Job;
import com.example.chronos.domain.enums.HttpMethodType;
import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.dto.job.JobCreateRequest;
import com.example.chronos.dto.job.JobResponse;
import com.example.chronos.dto.job.JobUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public Job toEntity(JobCreateRequest dto, String owner) {
        Job j = new Job();
        j.setName(dto.getName()); // ✅ ADD THIS LINE - IT'S MISSING!
        j.setTargetUrl(dto.getTargetUrl());
        j.setHttpMethod(HttpMethodType.valueOf(dto.getHttpMethod()));
        j.setRequestBody(dto.getRequestBody());
        j.setPriority(dto.getPriority() != null ? dto.getPriority() : 5); // ✅ Add null check
        j.setTimeoutSeconds(dto.getTimeoutSeconds() != null ? dto.getTimeoutSeconds() : 30); // ✅ Add null check
        j.setMaxRetries(dto.getMaxRetries() != null ? dto.getMaxRetries() : 3); // ✅ Add null check
        j.setBackoffSeconds(dto.getBackoffSeconds() != null ? dto.getBackoffSeconds() : 30); // ✅ Add null check
        j.setWebhookUrl(dto.getWebhookUrl());
        j.setCronExpression(dto.getCronExpression());
        j.setCreatedBy(owner);
        j.setStatus(JobStatus.SCHEDULED);
        return j;
    }

    public void updateEntity(Job job, JobUpdateRequest dto) {
        if (dto.getName() != null) job.setName(dto.getName());
        if (dto.getTargetUrl() != null) job.setTargetUrl(dto.getTargetUrl());
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
        r.setHttpMethod(job.getHttpMethod());
        r.setCronExpression(job.getCronExpression());
        r.setStatus(job.getStatus());
        r.setPriority(job.getPriority());
        r.setTimeoutSeconds(job.getTimeoutSeconds());
        r.setMaxRetries(job.getMaxRetries());
        r.setRetryCount(job.getRetryCount());
        r.setBackoffSeconds(job.getBackoffSeconds());
        r.setWebhookUrl(job.getWebhookUrl());
        r.setNextRunAt(job.getNextRunAt());
        r.setCreatedAt(job.getCreatedAt());
        r.setUpdatedAt(job.getUpdatedAt());
        r.setLastError(job.getLastError());
        return r;
    }
}