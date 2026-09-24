package ir.jetvam.apps.jobs.infrastructure.service;

/**
 * Carries editable scheduling and display properties for an existing job definition.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public record UpdateJobDefinitionCommand(
        String displayName,
        String description,
        String handlerKey,
        String cronExpression,
        String timeZone,
        boolean enabled
) {
}
