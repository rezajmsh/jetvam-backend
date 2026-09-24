package ir.jetvam.modules.settings.service;

import java.util.List;

/**
 * Read and update port for database-backed runtime settings.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public interface SettingService {
    boolean getBoolean(String key);
    SettingView get(String key);
    List<SettingView> findAll();
    SettingView update(String key, String value);
}
