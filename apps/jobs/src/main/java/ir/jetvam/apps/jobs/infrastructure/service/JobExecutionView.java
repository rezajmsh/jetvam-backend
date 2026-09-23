package ir.jetvam.apps.jobs.infrastructure.service;

import ir.jetvam.apps.jobs.infrastructure.model.JobExecutionStatus;
import ir.jetvam.apps.jobs.infrastructure.model.JobTriggerType;

import java.time.Instant;
import java.util.UUID;

public record JobExecutionView(
        UUID id,
        UUID definitionId,
        String jobCode,
        String handlerKey,
        JobTriggerType triggerType,
        JobExecutionStatus status,
        String requestedBy,
        Instant scheduledAt,
        Instant startedAt,
        Instant finishedAt,
        Long durationMs,
        long processedCount,
        long succeededCount,
        long failedCount,
        String resultSummary,
        String errorType,
        String errorMessage
) {
}
