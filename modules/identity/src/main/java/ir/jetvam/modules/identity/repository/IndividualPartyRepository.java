package ir.jetvam.modules.identity.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides persistence access to natural-person identity attributes.
 * National-code lookup enforces identity reuse across account categories.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public interface IndividualPartyRepository extends JetvamJpaRepository<IndividualPartyEntity, UUID> {

    Optional<IndividualPartyEntity> findByNationalCode(String nationalCode);

    Optional<IndividualPartyEntity> findByMobile(String mobile);

    boolean existsByNationalCode(String nationalCode);

    boolean existsByMobile(String mobile);
}
