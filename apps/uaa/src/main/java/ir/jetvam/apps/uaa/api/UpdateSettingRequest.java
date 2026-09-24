package ir.jetvam.apps.uaa.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Carries a setting value while its type remains controlled by persisted metadata.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public record UpdateSettingRequest(@NotBlank String value) {
}
