package ir.jetvam.apps.uaa.persistence;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;

import java.util.Optional;

/**
 * Persists OAuth clients through the centralized Jetvam JPA repository contract.
 * Disabled registrations are excluded from authorization-server lookups.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public interface OAuthClientJpaRepository extends JetvamJpaRepository<OAuthClientEntity, String> {

    Optional<OAuthClientEntity> findByIdAndEnabledTrue(String id);

    Optional<OAuthClientEntity> findByClientIdAndEnabledTrue(String clientId);
}
