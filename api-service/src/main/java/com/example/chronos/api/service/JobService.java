package com.example.chronos.api.service;

import com.example.chronos.api.dto.Dtos.*;
import com.example.chronos.api.web.ApiException;
import com.example.chronos.domain.Job;
import com.example.chronos.domain.JobInstance;
import com.example.chronos.domain.JobInstance.InstanceStatus;
import com.example.chronos.domain.enums.JobStatus;
import com.example.chronos.domain.enums.ScheduleType;
import com.example.chronos.repository.JobInstanceRepository;
import com.example.chronos.repository.JobRepository;
import com.example.chronos.service.JobLifecycleService;
import com.example.chronos.support.ScheduleCalculator;
import com.example.chronos.support.UrlSafetyValidator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every method takes the caller; ownership is enforced here, never trusted from the request body. */
@Service
@RequiredArgsConstructor
@Transactional
public class JobService {

    public record Caller(String username, boolean admin) {}

    private static final int MAX_PAGE = 200;

    private final JobRepository jobs;
    private final JobInstanceRepository instances;
    private final JobLifecycleService lifecycle;
    private final UrlSafetyValidator urlValidator;

    public JobResponse create(Caller c, JobRequest r) {
        Job j = new Job();
        j.setCreatedBy(c.username());
        apply(j, r);
        j.setNextRunAt(lifecycle.firstRun(j));
        return JobResponse.of(jobs.save(j));
    }

    public JobResponse update(Caller c, Long id, JobRequest r) {
        Job j = owned(c, id);
        apply(j, r);
        if (j.getStatus() == JobStatus.ACTIVE) {
            j.setNextRunAt(lifecycle.firstRun(j));
        }
        return JobResponse.of(jobs.save(j));
    }

    public void delete(Caller c, Long id) {
        jobs.delete(owned(c, id));
    }

    @Transactional(readOnly = true)
    public JobResponse get(Caller c, Long id) {
        return JobResponse.of(owned(c, id));
    }

    @Transactional(readOnly = true)
    public List<JobResponse> list(Caller c, int page, int size) {
        var pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE), Sort.by(Sort.Direction.DESC, "id"));
        Page<Job> p = c.admin() ? jobs.findAll(pr) : jobs.findByCreatedBy(c.username(), pr);
        return p.map(JobResponse::of).getContent();
    }

    public JobResponse pause(Caller c, Long id) {
        Job j = owned(c, id);
        if (j.getStatus() != JobStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "Only ACTIVE jobs can be paused (status is " + j.getStatus() + ")");
        }
        j.setStatus(JobStatus.PAUSED);
        return JobResponse.of(jobs.save(j));
    }

    public JobResponse resume(Caller c, Long id) {
        Job j = owned(c, id);
        if (j.getStatus() != JobStatus.PAUSED) {
            throw new ApiException(HttpStatus.CONFLICT, "Only PAUSED jobs can be resumed (status is " + j.getStatus() + ")");
        }
        j.setStatus(JobStatus.ACTIVE);
        j.setNextRunAt(lifecycle.firstRun(j)); // do not replay runs missed while paused
        return JobResponse.of(jobs.save(j));
    }

    public InstanceResponse trigger(Caller c, Long id) {
        Job j = owned(c, id);
        if (j.getStatus() == JobStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Job is cancelled");
        }
        return InstanceResponse.of(lifecycle.trigger(j));
    }

    public InstanceResponse retryInstance(Caller c, Long instanceId) {
        JobInstance i = instances.findWithJobById(instanceId).orElseThrow(() -> notFound("Instance"));
        Job j = owned(c, i.getJob().getId());
        if (!i.getStatus().isTerminal() || i.getStatus() == InstanceStatus.SUCCESS) {
            throw new ApiException(HttpStatus.CONFLICT, "Only failed runs can be retried");
        }
        return InstanceResponse.of(lifecycle.trigger(j));
    }

    @Transactional(readOnly = true)
    public List<InstanceResponse> instances(Caller c, Long jobId, InstanceStatus status, int page, int size) {
        owned(c, jobId);
        var pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE), Sort.by(Sort.Direction.DESC, "scheduledTime"));
        Page<JobInstance> p = status == null ? instances.findByJob_Id(jobId, pr) : instances.findByJob_IdAndStatus(jobId, status, pr);
        return p.map(InstanceResponse::of).getContent();
    }

    @Transactional(readOnly = true)
    public InstanceResponse instance(Caller c, Long instanceId) {
        return InstanceResponse.of(ownedInstance(c, instanceId));
    }

    @Transactional(readOnly = true)
    public JobInstance ownedInstance(Caller c, Long instanceId) {
        JobInstance i = instances.findWithJobById(instanceId).orElseThrow(() -> notFound("Instance"));
        if (!c.admin() && !i.getJob().getCreatedBy().equals(c.username())) {
            throw notFound("Instance"); // 404, not 403: don't reveal that other people's ids exist
        }
        return i;
    }

    @Transactional(readOnly = true)
    public JobStatistics statistics(Caller c, Long jobId) {
        Job j = owned(c, jobId);
        long total = j.getTotalRuns();
        double rate = total == 0 ? 0 : (double) j.getSuccessfulRuns() / total;
        return new JobStatistics(j.getId(), total, j.getSuccessfulRuns(), j.getFailedRuns(), rate, j.getAvgDurationMs(),
                j.getConsecutiveFailures(), j.getLastRunAt(), toMap(instances.countByStatusForJob(jobId)));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> jobStatusCounts(Caller c) {
        return toMap(c.admin() ? jobs.countByStatusAll() : jobs.countByStatusForOwner(c.username()));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> instanceStatusCounts(Caller c) {
        return toMap(c.admin() ? instances.countByStatusAll() : instances.countByStatusForOwner(c.username()));
    }

    private Job owned(Caller c, Long id) {
        var found = c.admin() ? jobs.findById(id) : jobs.findByIdAndCreatedBy(id, c.username());
        return found.orElseThrow(() -> notFound("Job"));
    }

    private void apply(Job j, JobRequest r) {
        ScheduleType type = r.scheduleType() == null ? ScheduleType.CRON : r.scheduleType();
        String tz = r.timezone() == null || r.timezone().isBlank() ? "UTC" : r.timezone();
        ScheduleCalculator.validate(type, r.cronExpression(), tz);
        urlValidator.validate(r.targetUrl());
        if (r.webhookUrl() != null && !r.webhookUrl().isBlank()) {
            urlValidator.validate(r.webhookUrl());
        }
        j.setName(r.name().trim());
        j.setDescription(r.description());
        j.setTargetUrl(r.targetUrl().trim());
        j.setHttpMethod(r.httpMethod());
        j.setRequestBody(r.requestBody());
        j.setRequestHeaders(r.requestHeaders());
        j.setScheduleType(type);
        j.setCronExpression(r.cronExpression().trim());
        j.setTimezone(tz);
        if (r.priority() != null) j.setPriority(r.priority());
        if (r.timeoutSeconds() != null) j.setTimeoutSeconds(r.timeoutSeconds());
        if (r.maxRetries() != null) j.setMaxRetries(r.maxRetries());
        if (r.backoffSeconds() != null) j.setBackoffSeconds(r.backoffSeconds());
        j.setWebhookUrl(r.webhookUrl() == null || r.webhookUrl().isBlank() ? null : r.webhookUrl().trim());
    }

    private static Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Object[] r : rows) {
            m.put(String.valueOf(r[0]), ((Number) r[1]).longValue());
        }
        return m;
    }

    private static ApiException notFound(String what) {
        return new ApiException(HttpStatus.NOT_FOUND, what + " not found");
    }
}
