package com.example.chronos.service;

import com.example.chronos.domain.JobInstance;
import com.example.chronos.domain.JobInstance.InstanceStatus;
import com.example.chronos.dto.job.JobInstanceDTO;
import com.example.chronos.dto.auth.PageResponse;
import com.example.chronos.repository.JobInstanceRepository;
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
public class JobInstanceService {

    private final JobInstanceRepository instanceRepository;

    /**
     * Find all instances with filtering
     */
    @Transactional(readOnly = true)
    public PageResponse<JobInstanceDTO> findAll(Pageable pageable, InstanceStatus status, Long jobId) {
        Page<JobInstance> page;

        if (jobId != null && status != null) {
            page = instanceRepository.findByJobIdAndStatus(jobId, status, pageable);
        } else if (jobId != null) {
            page = instanceRepository.findByJobId(jobId, pageable);
        } else if (status != null) {
            page = instanceRepository.findAll(
                    (root, query, cb) -> cb.equal(root.get("status"), status),
                    pageable);
        } else {
            page = instanceRepository.findAll(pageable);
        }

        return mapToPageResponse(page);
    }

    /**
     * Find instance by ID
     */
    @Transactional(readOnly = true)
    public JobInstanceDTO findById(Long id) {
        JobInstance instance = instanceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + id));
        return mapToDTO(instance);
    }

    /**
     * Find instances by job ID
     */
    @Transactional(readOnly = true)
    public PageResponse<JobInstanceDTO> findByJobId(Long jobId, InstanceStatus status, Pageable pageable) {
        Page<JobInstance> page = status != null
                ? instanceRepository.findByJobIdAndStatus(jobId, status, pageable)
                : instanceRepository.findByJobId(jobId, pageable);

        return mapToPageResponse(page);
    }

    /**
     * Find recent instances
     */
    @Transactional(readOnly = true)
    public PageResponse<JobInstanceDTO> findRecent(Pageable pageable) {
        Page<JobInstance> page = instanceRepository.findAll(pageable);
        return mapToPageResponse(page);
    }

    /**
     * Find instances by status
     */
    @Transactional(readOnly = true)
    public PageResponse<JobInstanceDTO> findByStatus(InstanceStatus status, Pageable pageable) {
        List<JobInstance> instances = instanceRepository.findByStatus(status);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), instances.size());

        List<JobInstanceDTO> dtos = instances.subList(start, end).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return PageResponse.<JobInstanceDTO>builder()
                .content(dtos)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements((long) instances.size())
                .totalPages((int) Math.ceil((double) instances.size() / pageable.getPageSize()))
                .build();
    }

    /**
     * Find instances by date range
     */
    @Transactional(readOnly = true)
    public PageResponse<JobInstanceDTO> findByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        List<JobInstance> instances = instanceRepository.findByScheduledTimeBetween(start, end);

        int pageStart = (int) pageable.getOffset();
        int pageEnd = Math.min((pageStart + pageable.getPageSize()), instances.size());

        List<JobInstanceDTO> dtos = instances.subList(pageStart, pageEnd).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return PageResponse.<JobInstanceDTO>builder()
                .content(dtos)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements((long) instances.size())
                .totalPages((int) Math.ceil((double) instances.size() / pageable.getPageSize()))
                .build();
    }

    /**
     * Find latest instance for a job
     */
    @Transactional(readOnly = true)
    public JobInstanceDTO findLatestByJobId(Long jobId) {
        JobInstance instance = instanceRepository.findFirstByJobIdOrderByCreatedAtDesc(jobId)
                .orElseThrow(() -> new IllegalArgumentException("No instances found for job: " + jobId));
        return mapToDTO(instance);
    }

    /**
     * Count instances by status
     */
    @Transactional(readOnly = true)
    public Map<String, Long> countByStatus(Long jobId) {
        Map<String, Long> counts = new HashMap<>();

        if (jobId != null) {
            for (InstanceStatus status : InstanceStatus.values()) {
                long count = instanceRepository.countByJobIdAndStatus(jobId, status);
                counts.put(status.name(), count);
            }
        } else {
            for (InstanceStatus status : InstanceStatus.values()) {
                long count = instanceRepository.findByStatus(status).size();
                counts.put(status.name(), count);
            }
        }

        return counts;
    }

    /**
     * Delete an instance
     */
    @Transactional
    public void deleteInstance(Long id) {
        if (!instanceRepository.existsById(id)) {
            throw new IllegalArgumentException("Instance not found: " + id);
        }
        instanceRepository.deleteById(id);
        log.info("Deleted instance: {}", id);
    }

    /**
     * Cleanup old instances
     */
    @Transactional
    public int cleanupOldInstances(int olderThanDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(olderThanDays);

        var oldInstances = instanceRepository.findAll().stream()
                .filter(i -> i.getCreatedAt().isBefore(cutoffDate))
                .collect(Collectors.toList());

        int count = oldInstances.size();
        instanceRepository.deleteAll(oldInstances);

        log.info("Cleaned up {} instances older than {} days", count, olderThanDays);
        return count;
    }

    /**
     * Map entity to DTO
     */
    private JobInstanceDTO mapToDTO(JobInstance instance) {
        JobInstanceDTO dto = JobInstanceDTO.builder()
                .id(instance.getId())
                .jobId(instance.getJob().getId())
                .jobName(instance.getJob().getName())
                .status(instance.getStatus())
                .scheduledTime(instance.getScheduledTime())
                .startedAt(instance.getStartedAt())
                .completedAt(instance.getCompletedAt())
                .durationMs(instance.getDurationMs())
                .httpStatusCode(instance.getHttpStatusCode())
                .responseBody(instance.getResponseBody())
                .errorMessage(instance.getErrorMessage())
                .retryCount(instance.getRetryCount())
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .build();

        dto.computeDerivedFields();
        return dto;
    }

    /**
     * Map page to page response
     */
    private PageResponse<JobInstanceDTO> mapToPageResponse(Page<JobInstance> page) {
        List<JobInstanceDTO> dtos = page.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return PageResponse.<JobInstanceDTO>builder()
                .content(dtos)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}