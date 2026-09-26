package ir.jetvam.modules.origination.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.origination.model.LoanApplicationEntity;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists loan applications and provides customer-scoped lookup methods.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface LoanApplicationRepository extends JetvamJpaRepository<LoanApplicationEntity, UUID> {

    List<LoanApplicationEntity> findAllByCustomerPartyIdOrderByCreatedAtDesc(UUID customerPartyId);

    Optional<LoanApplicationEntity> findByIdAndCustomerPartyId(UUID id, UUID customerPartyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select application from LoanApplicationEntity application where application.id = :applicationId")
    Optional<LoanApplicationEntity> findByIdForUpdate(@Param("applicationId") UUID applicationId);
}
