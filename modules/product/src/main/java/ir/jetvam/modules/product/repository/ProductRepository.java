package ir.jetvam.modules.product.repository;

import ir.jetvam.modules.product.model.ProductEntity;
import ir.jetvam.modules.product.model.PublicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists product families and provides catalog-oriented lookup operations.
 * Plan configuration is accessed through its own aggregate repository.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    boolean existsByCode(String code);

    Optional<ProductEntity> findByCode(String code);

    List<ProductEntity> findAllByOrderByNameAsc();

    List<ProductEntity> findAllByStatusOrderByNameAsc(PublicationStatus status);
}
