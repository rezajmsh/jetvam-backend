package ir.jetvam.apps.jobs.infrastructure.repository;

/**
 * Database projection for cumulative counters of one job definition.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public interface JobExecutionStatistics {

    long getExecutionCount();

    long getSucceededExecutionCount();

    long getFailedExecutionCount();

    long getProcessedCount();

    long getSucceededCount();

    long getFailedCount();
}
