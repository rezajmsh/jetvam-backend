package ir.jetvam.apps.jobs.infrastructure.service;

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
