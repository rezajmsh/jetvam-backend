package ir.jetvam.apps.jobs.infrastructure.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Exposes the operational configuration and next execution time of a managed job.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public record JobDefinitionView(
        UUID id,
        String code,
        String displayName,
        String description,
        String handlerKey,
        String cronExpression,
        String timeZone,
        boolean enabled,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
