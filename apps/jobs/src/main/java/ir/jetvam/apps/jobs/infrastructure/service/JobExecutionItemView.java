package ir.jetvam.apps.jobs.infrastructure.service;

import ir.jetvam.apps.jobs.infrastructure.model.JobExecutionItemStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Exposes one business item processed in a job execution.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
public record JobExecutionItemView(
        UUID id,
        UUID executionId,
        int sequenceNumber,
        String itemType,
        String itemKey,
        String operationCode,
        String subjectIdentifier,
        String subjectKey,
        JobExecutionItemStatus status,
        String businessStatus,
        String providerCode,
        String externalReference,
        String message,
        Instant createdAt
) {
}
