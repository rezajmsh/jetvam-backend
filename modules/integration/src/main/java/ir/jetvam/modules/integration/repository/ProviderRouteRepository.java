package ir.jetvam.modules.integration.repository;

import ir.jetvam.modules.integration.persistence.ProviderRouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persists capability-level routing policies and manual overrides.
 * Capability codes form the stable lookup key used on every provider invocation.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public interface ProviderRouteRepository extends JpaRepository<ProviderRouteEntity, UUID> {

    Optional<ProviderRouteEntity> findByCapabilityCode(String capabilityCode);
}
