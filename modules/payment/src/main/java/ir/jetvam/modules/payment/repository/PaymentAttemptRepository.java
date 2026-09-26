package ir.jetvam.modules.payment.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.payment.model.PaymentAttemptEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Persists idempotent payment attempts and provider confirmations.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface PaymentAttemptRepository extends JetvamJpaRepository<PaymentAttemptEntity, UUID> {

    Optional<PaymentAttemptEntity> findByIdempotencyKey(String idempotencyKey);
}
