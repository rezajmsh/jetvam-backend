package ir.jetvam.modules.payment.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.payment.model.FeeCategory;
import ir.jetvam.modules.payment.model.FeeObligationEntity;
import ir.jetvam.modules.payment.model.FeeStatus;

import java.util.List;
import java.util.UUID;

/**
 * Persists fee obligations and supports workflow payment gates.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface FeeObligationRepository extends JetvamJpaRepository<FeeObligationEntity, UUID> {

    List<FeeObligationEntity> findAllByReferenceTypeAndReferenceIdOrderByFeeCode(String referenceType, UUID referenceId);

    boolean existsByReferenceTypeAndReferenceIdAndActivationKeyAndStatusNot(
            String referenceType,
            UUID referenceId,
            String activationKey,
            FeeStatus status
    );

    boolean existsByReferenceTypeAndReferenceIdAndCategoryAndStatusNot(
            String referenceType,
            UUID referenceId,
            FeeCategory category,
            FeeStatus status
    );

    boolean existsByReferenceTypeAndReferenceIdAndFeeCode(String referenceType, UUID referenceId, String feeCode);
}
