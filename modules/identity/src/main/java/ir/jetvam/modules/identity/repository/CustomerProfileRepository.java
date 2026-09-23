package ir.jetvam.modules.identity.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.identity.persistence.CustomerProfileEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Persists customer registration and Shahkar verification progress.
 * Profiles are located through the stable party identifier.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public interface CustomerProfileRepository extends JetvamJpaRepository<CustomerProfileEntity, UUID> {

    Optional<CustomerProfileEntity> findByPartyId(UUID partyId);
}
