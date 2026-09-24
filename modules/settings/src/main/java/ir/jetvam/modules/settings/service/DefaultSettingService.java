package ir.jetvam.modules.settings.service;

import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.settings.model.SettingValueType;
import ir.jetvam.modules.settings.persistence.SystemSettingEntity;
import ir.jetvam.modules.settings.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implements typed setting access while retaining the value's declared schema.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultSettingService implements SettingService {

    private final SystemSettingRepository repository;

    @Override
    @Transactional(readOnly = true)
    public boolean getBoolean(String key) {
        SystemSettingEntity setting = find(key);
        Preconditions.require(setting.getValueType() == SettingValueType.BOOLEAN,
                "setting is not boolean: " + key);
        return Boolean.parseBoolean(setting.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public SettingView get(String key) {
        return toView(find(key));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SettingView> findAll() {
        return repository.findAll().stream().map(DefaultSettingService::toView).toList();
    }

    @Override
    @Transactional
    public SettingView update(String key, String value) {
        SystemSettingEntity setting = find(key);
        setting.changeValue(value);
        return toView(setting);
    }

    private SystemSettingEntity find(String key) {
        String normalized = Preconditions.requireText(key, "key").strip();
        return repository.findByKey(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("setting", normalized));
    }

    private static SettingView toView(SystemSettingEntity setting) {
        return new SettingView(
                setting.getId(), setting.getKey(), setting.getValue(),
                setting.getValueType(), setting.getDescription()
        );
    }
}
