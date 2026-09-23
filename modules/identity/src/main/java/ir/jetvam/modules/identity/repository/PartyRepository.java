package ir.jetvam.modules.identity.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.identity.persistence.PartyEntity;

import java.util.UUID;

/**
 * Persists canonical party roots used by every identity profile.
 * Domain services coordinate this repository with type-specific records.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public interface PartyRepository extends JetvamJpaRepository<PartyEntity, UUID> {
}
