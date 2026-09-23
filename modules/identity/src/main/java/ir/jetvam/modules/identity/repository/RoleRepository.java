package ir.jetvam.modules.identity.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.identity.persistence.RoleEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves data-driven roles and their permission assignments.
 * Entity graphs ensure token creation can access permissions safely.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public interface RoleRepository extends JetvamJpaRepository<RoleEntity, UUID> {

    List<RoleEntity> findAllByCodeIn(Collection<String> codes);

    Optional<RoleEntity> findByCode(String code);
}
