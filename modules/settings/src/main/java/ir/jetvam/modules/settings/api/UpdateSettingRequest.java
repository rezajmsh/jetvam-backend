package ir.jetvam.modules.settings.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Carries a setting value whose target type is controlled by persisted metadata.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public record UpdateSettingRequest(@NotBlank String value) {
}
