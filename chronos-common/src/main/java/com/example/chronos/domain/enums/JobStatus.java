package com.example.chronos.domain.enums;

/**
 * Lifecycle of a {@code Job} (the definition), NOT of one execution.
 *
 * <ul>
 *   <li>{@link #ACTIVE}    - the scheduler will create executions when {@code nextRunAt} is due</li>
 *   <li>{@link #PAUSED}    - no new executions are created; can be resumed</li>
 *   <li>{@link #COMPLETED} - a ONCE job that has fired, or a schedule with no further runs</li>
 *   <li>{@link #CANCELLED} - permanently stopped</li>
 * </ul>
 *
 * Execution state (running/failed/...) lives in {@code JobInstance.InstanceStatus}.
 * The old SCHEDULED/PENDING/RUNNING/FAILED/INACTIVE job states mixed both concepts
 * and caused the scheduler to miss jobs the API had created.
 */
public enum JobStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED
}
