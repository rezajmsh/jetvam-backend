package ir.jetvam.modules.settings.service;

import ir.jetvam.modules.settings.model.SettingValueType;

import java.util.UUID;

/**
 * Safe representation used by setting-management APIs.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public record SettingView(
        UUID id,
        String key,
        String value,
        SettingValueType valueType,
        String description
) {
}
