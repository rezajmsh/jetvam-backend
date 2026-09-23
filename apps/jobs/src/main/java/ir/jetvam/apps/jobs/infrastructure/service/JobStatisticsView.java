package ir.jetvam.apps.jobs.infrastructure.service;

import java.util.UUID;

/** Cumulative execution and item-level statistics for one managed job. */
public record JobStatisticsView(
        UUID definitionId,
        long executionCount,
        long succeededExecutionCount,
        long failedExecutionCount,
        long processedCount,
        long succeededCount,
        long failedCount
) {
}
