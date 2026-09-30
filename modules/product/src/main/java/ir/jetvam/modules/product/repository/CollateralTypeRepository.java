package ir.jetvam.modules.product.repository;

import ir.jetvam.modules.product.model.CollateralTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Persists centrally managed collateral types.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public interface CollateralTypeRepository extends JpaRepository<CollateralTypeEntity, UUID> {
    boolean existsByCode(String code);
    List<CollateralTypeEntity> findAllByOrderByTitleAsc();
}
