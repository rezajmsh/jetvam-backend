package ir.jetvam.apps.jobs.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Receives a new job definition and its initial schedule from the management API.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public record CreateJobRequest(
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 200) String displayName,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 150) String handlerKey,
        @NotBlank @Size(max = 120) String cronExpression,
        @NotBlank @Size(max = 80) String timeZone,
        boolean enabled
) {
}
