package ir.jetvam.apps.jobs.infrastructure.service;

import ir.jetvam.apps.jobs.infrastructure.model.JobExecutionItemStatus;

/**
 * Generic item snapshot supplied by a job handler after processing one business record.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
public record JobExecutionItemCommand(
        String itemType,
        String itemKey,
        String operationCode,
        String subjectIdentifier,
        String subjectKey,
        JobExecutionItemStatus status,
        String businessStatus,
        String providerCode,
        String externalReference,
        String message
) {
}
