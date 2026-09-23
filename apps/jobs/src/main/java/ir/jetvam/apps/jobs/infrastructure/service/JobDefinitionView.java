package ir.jetvam.apps.jobs.infrastructure.service;

import java.time.Instant;
import java.util.UUID;

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
