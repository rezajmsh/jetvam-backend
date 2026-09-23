package ir.jetvam.modules.identity.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.identity.persistence.OrganizationPartyEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides persistence access to legal-organization identity records.
 * Merchant modules can reference parties without owning duplicate legal data.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public interface OrganizationPartyRepository extends JetvamJpaRepository<OrganizationPartyEntity, UUID> {

    Optional<OrganizationPartyEntity> findByNationalId(String nationalId);
}
