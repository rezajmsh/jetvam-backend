package ir.jetvam.apps.jobs.infrastructure.repository;

import ir.jetvam.apps.jobs.infrastructure.persistence.JobDefinitionEntity;
import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists managed job definitions and resolves them by their stable code.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public interface JobDefinitionRepository extends JetvamJpaRepository<JobDefinitionEntity, UUID> {

    Optional<JobDefinitionEntity> findByCode(String code);

    boolean existsByCode(String code);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update JobDefinitionEntity definition
               set definition.lockOwner = :owner, definition.lockUntil = :lockUntil
             where definition.id = :id
               and (definition.lockUntil is null or definition.lockUntil < :now)
            """)
    int tryAcquireExecutionLock(
            @Param("id") UUID id,
            @Param("owner") String owner,
            @Param("now") Instant now,
            @Param("lockUntil") Instant lockUntil
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update JobDefinitionEntity definition
               set definition.lockOwner = null, definition.lockUntil = null
             where definition.id = :id and definition.lockOwner = :owner
            """)
    int releaseExecutionLock(@Param("id") UUID id, @Param("owner") String owner);
}
