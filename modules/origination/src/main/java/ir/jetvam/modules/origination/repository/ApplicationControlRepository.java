package ir.jetvam.modules.origination.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.origination.model.ApplicationControlEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Persists application control snapshots and resolves their owning aggregate for locked callbacks.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface ApplicationControlRepository extends JetvamJpaRepository<ApplicationControlEntity, UUID> {

    @Query("select control.application.id from ApplicationControlEntity control where control.id = :controlId")
    Optional<UUID> findApplicationId(@Param("controlId") UUID controlId);
}
