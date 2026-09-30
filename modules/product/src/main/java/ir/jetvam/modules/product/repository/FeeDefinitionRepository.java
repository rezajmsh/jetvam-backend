package ir.jetvam.modules.product.repository;

import ir.jetvam.modules.product.model.FeeDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Persists reusable fee definitions selected by plans.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public interface FeeDefinitionRepository extends JpaRepository<FeeDefinitionEntity, UUID> {
    boolean existsByCode(String code);
    List<FeeDefinitionEntity> findAllByOrderByTitleAsc();
}
