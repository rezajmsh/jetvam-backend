package ir.jetvam.modules.integration.repository;

import ir.jetvam.modules.integration.persistence.ExternalProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides runtime and administrative access to external provider definitions.
 * Enabled candidates are loaded by capability before routing and health filtering.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public interface ExternalProviderRepository extends JpaRepository<ExternalProviderEntity, UUID> {

    List<ExternalProviderEntity> findAllByCapabilityCodeOrderByPriorityAscProviderCodeAsc(String capabilityCode);

    List<ExternalProviderEntity> findAllByCapabilityCodeAndEnabledTrueOrderByPriorityAscProviderCodeAsc(
            String capabilityCode
    );

    Optional<ExternalProviderEntity> findByCapabilityCodeAndProviderCode(String capabilityCode, String providerCode);
}
