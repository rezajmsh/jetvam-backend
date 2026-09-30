package ir.jetvam.modules.product.repository;

import ir.jetvam.modules.product.model.DocumentTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Persists centrally managed document types used by collateral and employment requirements.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public interface DocumentTypeRepository extends JpaRepository<DocumentTypeEntity, UUID> {
    boolean existsByCode(String code);
    List<DocumentTypeEntity> findAllByOrderByTitleAsc();
}
