package ir.jetvam.modules.product.repository;

import ir.jetvam.modules.product.model.PlanEntity;
import ir.jetvam.modules.product.model.PublicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists plan aggregates including their declarative requirements and fees.
 * Service transactions initialize and map child configuration before returning API views.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface PlanRepository extends JpaRepository<PlanEntity, UUID> {

    boolean existsByProductIdAndCode(UUID productId, String code);

    List<PlanEntity> findAllByProductIdOrderByNameAsc(UUID productId);

    List<PlanEntity> findAllByProductIdAndStatusOrderByNameAsc(UUID productId, PublicationStatus status);

    Optional<PlanEntity> findByIdAndStatus(UUID id, PublicationStatus status);
}
