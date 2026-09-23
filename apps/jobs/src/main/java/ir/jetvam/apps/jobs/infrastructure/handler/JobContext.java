package ir.jetvam.apps.jobs.infrastructure.handler;

import ir.jetvam.apps.jobs.infrastructure.model.JobTriggerType;

import java.time.Instant;
import java.util.UUID;

/** Context shared with handlers without exposing Quartz or persistence types. */
public record JobContext(
        UUID executionId,
        UUID definitionId,
        String jobCode,
        JobTriggerType triggerType,
        String requestedBy,
        Instant scheduledAt,
        Instant startedAt
) {
}
