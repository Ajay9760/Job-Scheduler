package com.example.chronos.api.web;

import com.example.chronos.api.dto.Dtos.*;
import com.example.chronos.api.service.AuthService;
import com.example.chronos.api.service.JobService;
import com.example.chronos.api.service.JobService.Caller;
import com.example.chronos.api.service.LogService;
import com.example.chronos.domain.JobInstance.InstanceStatus;
import com.example.chronos.domain.JobLog.LogLevel;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** Thin controllers: the caller identity ALWAYS comes from the authenticated principal. */
public final class Controllers {
    private Controllers() {}

    static Caller caller(Authentication a) {
        boolean admin = a.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_ADMIN"));
        return new Caller(a.getName(), admin);
    }

    @RestController
    @RequestMapping("/api/auth")
    @RequiredArgsConstructor
    public static class AuthController {
        private final AuthService auth;

        @PostMapping("/login")
        public TokenResponse login(@Valid @RequestBody LoginRequest r) {
            return auth.login(r);
        }

        @PostMapping("/register")
        @ResponseStatus(HttpStatus.CREATED)
        public TokenResponse register(@Valid @RequestBody RegisterRequest r) {
            return auth.register(r);
        }

        @PostMapping("/logout-all")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void logoutAll(Authentication a) {
            auth.revokeTokens(a.getName());
        }
    }

    @RestController
    @RequestMapping("/api/jobs")
    @RequiredArgsConstructor
    public static class JobController {
        private final JobService jobs;

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public JobResponse create(Authentication a, @Valid @RequestBody JobRequest r) {
            return jobs.create(caller(a), r);
        }

        @GetMapping
        public List<JobResponse> list(Authentication a, @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "100") int size) {
            return jobs.list(caller(a), page, size);
        }

        @GetMapping("/{id}")
        public JobResponse get(Authentication a, @PathVariable Long id) {
            return jobs.get(caller(a), id);
        }

        @PutMapping("/{id}")
        public JobResponse update(Authentication a, @PathVariable Long id, @Valid @RequestBody JobRequest r) {
            return jobs.update(caller(a), id, r);
        }

        @DeleteMapping("/{id}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void delete(Authentication a, @PathVariable Long id) {
            jobs.delete(caller(a), id);
        }

        @PostMapping("/{id}/trigger")
        @ResponseStatus(HttpStatus.ACCEPTED)
        public InstanceResponse trigger(Authentication a, @PathVariable Long id) {
            return jobs.trigger(caller(a), id);
        }

        @PostMapping("/{id}/pause")
        public JobResponse pause(Authentication a, @PathVariable Long id) {
            return jobs.pause(caller(a), id);
        }

        @PostMapping("/{id}/resume")
        public JobResponse resume(Authentication a, @PathVariable Long id) {
            return jobs.resume(caller(a), id);
        }

        @GetMapping("/{id}/instances")
        public List<InstanceResponse> instances(Authentication a, @PathVariable Long id,
                                                @RequestParam(required = false) InstanceStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
            return jobs.instances(caller(a), id, status, page, size);
        }

        @PostMapping("/instances/{instanceId}/retry")
        @ResponseStatus(HttpStatus.ACCEPTED)
        public InstanceResponse retry(Authentication a, @PathVariable Long instanceId) {
            return jobs.retryInstance(caller(a), instanceId);
        }
    }

    @RestController
    @RequestMapping("/api/instances")
    @RequiredArgsConstructor
    public static class InstanceController {
        private final JobService jobs;

        @GetMapping("/{id}")
        public InstanceResponse get(Authentication a, @PathVariable Long id) {
            return jobs.instance(caller(a), id);
        }

        @GetMapping("/job/{jobId}")
        public List<InstanceResponse> byJob(Authentication a, @PathVariable Long jobId,
                                            @RequestParam(required = false) InstanceStatus status,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "50") int size) {
            return jobs.instances(caller(a), jobId, status, page, size);
        }
    }

    @RestController
    @RequestMapping("/api/logs")
    @RequiredArgsConstructor
    public static class LogController {
        private final LogService logs;

        @GetMapping
        public PageResponse<LogResponse> mine(Authentication a, @RequestParam(required = false) LogLevel level,
                                              @RequestParam(required = false) String q,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "50") int size) {
            return logs.mine(caller(a), level, q, page, size);
        }

        @GetMapping("/instance/{instanceId}")
        public PageResponse<LogResponse> byInstance(Authentication a, @PathVariable Long instanceId,
                                                    @RequestParam(required = false) LogLevel level,
                                                    @RequestParam(required = false) String q,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "50") int size) {
            return logs.forInstance(caller(a), instanceId, level, q, page, size);
        }
    }

    @RestController
    @RequiredArgsConstructor
    public static class StatsController {
        private final JobService jobs;

        @GetMapping("/api/statistics/job/{jobId}")
        public JobStatistics job(Authentication a, @PathVariable Long jobId) {
            return jobs.statistics(caller(a), jobId);
        }

        @GetMapping("/api/analytics/status-counts")
        public Map<String, Long> jobStatusCounts(Authentication a) {
            return jobs.jobStatusCounts(caller(a));
        }

        @GetMapping("/api/analytics/instance-status-counts")
        public Map<String, Long> instanceStatusCounts(Authentication a) {
            return jobs.instanceStatusCounts(caller(a));
        }
    }
}
