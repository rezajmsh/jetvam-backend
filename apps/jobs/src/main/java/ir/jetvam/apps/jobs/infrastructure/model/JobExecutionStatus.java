package ir.jetvam.apps.jobs.infrastructure.model;

/**
 * Represents the lifecycle state recorded for a managed job execution.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public enum JobExecutionStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED
}
