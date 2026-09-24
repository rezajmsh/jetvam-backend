package ir.jetvam.modules.settings.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.settings.persistence.SystemSettingEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides persistence access to runtime settings by their stable keys.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public interface SystemSettingRepository extends JetvamJpaRepository<SystemSettingEntity, UUID> {
    Optional<SystemSettingEntity> findByKey(String key);
}
