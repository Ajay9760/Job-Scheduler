package com.example.chronos.api.service;

import com.example.chronos.api.dto.Dtos.LogResponse;
import com.example.chronos.api.dto.Dtos.PageResponse;
import com.example.chronos.domain.JobLog.LogLevel;
import com.example.chronos.repository.JobLogRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogService {

    private static final Duration SEARCH_WINDOW = Duration.ofDays(30); // bounds the LIKE scan

    private final JobLogRepository logs;
    private final JobService jobService;

    public PageResponse<LogResponse> forInstance(JobService.Caller c, Long instanceId, LogLevel level, String q, int page, int size) {
        jobService.ownedInstance(c, instanceId); // ownership check
        var pr = pr(page, size);
        var p = q != null && !q.isBlank()
                ? logs.searchByInstance(instanceId, q.trim(), Instant.now().minus(SEARCH_WINDOW), pr)
                : level != null ? logs.findByInstanceIdAndLogLevel(instanceId, level, pr) : logs.findByInstanceId(instanceId, pr);
        return PageResponse.of(p.map(LogResponse::of));
    }

    public PageResponse<LogResponse> mine(JobService.Caller c, LogLevel level, String q, int page, int size) {
        var pr = pr(page, size);
        var p = q != null && !q.isBlank()
                ? (c.admin() ? logs.searchAll(q.trim(), Instant.now().minus(SEARCH_WINDOW), pr)
                             : logs.searchOwned(c.username(), q.trim(), Instant.now().minus(SEARCH_WINDOW), pr))
                : c.admin() ? (level != null ? logs.findByLogLevel(level, pr) : logs.findAll(pr))
                            : (level != null ? logs.findOwnedByLevel(c.username(), level, pr) : logs.findOwned(c.username(), pr));
        return PageResponse.of(p.map(LogResponse::of));
    }

    private static PageRequest pr(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
