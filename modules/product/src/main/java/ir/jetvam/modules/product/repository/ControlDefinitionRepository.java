package ir.jetvam.modules.product.repository;

import ir.jetvam.modules.product.model.ControlDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Persists reusable control definitions and their parameter metadata.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public interface ControlDefinitionRepository extends JpaRepository<ControlDefinitionEntity, UUID> {
    List<ControlDefinitionEntity> findAllByOrderByTitleAsc();
}
