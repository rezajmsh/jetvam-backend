package ir.jetvam.apps.jobs.infrastructure.service;

/**
 * Carries the immutable identity and initial schedule of a new managed job.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public record CreateJobDefinitionCommand(
        String code,
        String displayName,
        String description,
        String handlerKey,
        String cronExpression,
        String timeZone,
        boolean enabled
) {
}
