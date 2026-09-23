package ir.jetvam.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.NoRepositoryBean;

import jakarta.persistence.LockModeType;
import java.util.Optional;

/**
 * Defines the common Spring Data repository contract for Jetvam persistence adapters.
 * Business repositories inherit centralized conventions through this boundary.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@NoRepositoryBean
public interface JetvamJpaRepository<T, ID> extends JpaRepository<T, ID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select entity from #{#entityName} entity where entity.id = :id")
    Optional<T> findByIdForUpdate(@Param("id") ID id);
}
