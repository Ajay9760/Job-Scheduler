package com.example.chronos.service;


import com.example.chronos.domain.JobLog;
import com.example.chronos.dto.auth.PageResponse;
import com.example.chronos.dto.job.JobLogDTO;
import com.example.chronos.repository.JobLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobLogService {

    private final JobLogRepository logRepository;

    // Find logs by instance ID
    @Transactional(readOnly = true)
    public List<JobLogDTO> findByInstanceId(Long instanceId, JobLog.LogLevel level) {
        List<JobLog> logs = level != null
                ? logRepository.findByInstanceIdAndLogLevel(instanceId, level)
                : logRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId);

        return logs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Finds logs by job ID

    @Transactional(readOnly = true)
    public PageResponse<JobLogDTO> findByJobId(Long jobId, JobLog.LogLevel level, Pageable pageable) {
        Page<JobLog> page = level != null
                ? logRepository.findAll(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("job").get("id"), jobId),
                        cb.equal(root.get("logLevel"), level)
                ),
                pageable)
                : logRepository.findByJob_Id(jobId, pageable);

        return mapToPageResponse(page);
    }
    public Page<JobLog> findByJob_Id(Long jobId, JobLog.LogLevel level, Pageable pageable) {
        if (level != null) {
            return logRepository.findByJob_IdAndLogLevel(jobId, level, pageable);
        }
        return logRepository.findByJob_Id(jobId, pageable);
    }

    // Finds error logs

    @Transactional(readOnly = true)
    public PageResponse<JobLogDTO> findErrorLogs(Long jobId, Pageable pageable) {
        Page<JobLog> page = jobId != null
                ? logRepository.findErrorLogsByJobId(jobId, pageable)
                : logRepository.findByLogLevel(JobLog.LogLevel.ERROR, pageable);

        return mapToPageResponse(page);
    }

    // Search logs by message

    @Transactional(readOnly = true)
    public List<JobLogDTO> searchLogs(Long instanceId, String query) {
        List<JobLog> logs = logRepository.searchLogsByMessage(instanceId, query);
        return logs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<JobLogDTO> searchLogs(String query) {
        List<JobLog> logs = logRepository.searchAllLogsByMessage(query);
        return logs.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    //Get log statistics by job ID

    @Transactional(readOnly = true)
    public Map<String, Long> getStatisticsByJobId(Long jobId) {
        List<Object[]> results = logRepository.getLogStatisticsByJobId(jobId);

        Map<String, Long> stats = new HashMap<>();
        for (Object[] result : results) {
            JobLog.LogLevel level = (JobLog.LogLevel) result[0];
            Long count = (Long) result[1];
            stats.put(level.name(), count);
        }
        for (JobLog.LogLevel level : JobLog.LogLevel.values()) {
            stats.putIfAbsent(level.name(), 0L);
        }

        return stats;
    }

    //Find recent logs
    @Transactional(readOnly = true)
    public PageResponse<JobLogDTO> findRecent(Pageable pageable) {
        Page<JobLog> page = logRepository.findAll(pageable);
        return mapToPageResponse(page);
    }

    //Export logs as text

    @Transactional(readOnly = true)
    public String exportLogsAsText(Long instanceId) {
        List<JobLog> logs = logRepository.findByInstanceIdOrderByCreatedAtAsc(instanceId);

        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(80)).append("\n");
        sb.append("Job Instance Logs - Instance ID: ").append(instanceId).append("\n");
        sb.append("Generated at: ").append(LocalDateTime.now()).append("\n");
        sb.append("=".repeat(80)).append("\n\n");

        for (JobLog log : logs) {
            sb.append("[").append(log.getCreatedAt()).append("] ");
            sb.append("[").append(log.getLogLevel()).append("] ");
            sb.append(log.getMessage()).append("\n");

            if (log.getDetails() != null && !log.getDetails().isEmpty()) {
                sb.append("  Details: ").append(log.getDetails()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("=".repeat(80)).append("\n");
        sb.append("Total Logs: ").append(logs.size()).append("\n");

        return sb.toString();
    }

    // Cleanup old logs

    @Transactional
    public int cleanupOldLogs(int olderThanDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);

        var oldLogs = logRepository.findAll().stream()
                .filter(l -> l.getCreatedAt().isBefore(cutoffDate))
                .collect(Collectors.toList());

        int count = oldLogs.size();
        logRepository.deleteAll(oldLogs);

        log.info("Cleaned up {} logs older than {} days", count, olderThanDays);
        return count;
    }

    // Find log by ID

    @Transactional(readOnly = true)
    public JobLogDTO findById(Long id) {
        JobLog log = logRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Log not found: " + id));
        return mapToDTO(log);
    }

    // Map entity to DTO

    private JobLogDTO mapToDTO(JobLog log) {
        JobLogDTO dto = JobLogDTO.builder()
                .id(log.getId())
                .instanceId(log.getInstance().getId())
                .jobId(log.getJob().getId())
                .logLevel(log.getLogLevel())
                .message(log.getMessage())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();

        dto.computeUIFields();
        return dto;
    }

    // Map page to page response

    private PageResponse<JobLogDTO> mapToPageResponse(Page<JobLog> page) {
        List<JobLogDTO> dtos = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return PageResponse.<JobLogDTO>builder()
                .content(dtos)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
