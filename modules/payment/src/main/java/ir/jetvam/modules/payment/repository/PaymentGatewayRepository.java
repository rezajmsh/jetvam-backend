package ir.jetvam.modules.payment.repository;

import ir.jetvam.modules.payment.model.PaymentGatewayEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves active gateway configuration for checkout and back-office management.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public interface PaymentGatewayRepository extends JpaRepository<PaymentGatewayEntity, UUID> {
    Optional<PaymentGatewayEntity> findFirstByActiveTrueOrderByGatewayCodeAsc();
    Optional<PaymentGatewayEntity> findByGatewayCode(String gatewayCode);
    List<PaymentGatewayEntity> findAllByOrderByGatewayCodeAsc();
}
