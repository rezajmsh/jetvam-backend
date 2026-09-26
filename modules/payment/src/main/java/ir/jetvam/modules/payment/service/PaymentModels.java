package ir.jetvam.modules.payment.service;

import ir.jetvam.modules.payment.model.FeeCategory;
import ir.jetvam.modules.payment.model.FeeStatus;
import ir.jetvam.modules.payment.model.PaymentAttemptStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Groups payment commands and views exchanged across module and HTTP boundaries.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class PaymentModels {

    public static final String LOAN_APPLICATION = "LOAN_APPLICATION";
    public static final String APPLICATION_APPROVED = "APPLICATION_APPROVED";

    private PaymentModels() {
    }

    public record FeeDefinition(
            String code,
            String title,
            FeeCategory category,
            BigDecimal amount,
            String currency,
            String activationKey
    ) {
    }

    public record FeeView(
            UUID id,
            UUID referenceId,
            String code,
            String title,
            FeeCategory category,
            BigDecimal amount,
            String currency,
            String activationKey,
            FeeStatus status,
            Instant paidAt
    ) {
    }

    public record AttemptView(
            UUID id,
            UUID feeId,
            PaymentAttemptStatus status,
            String providerReference
    ) {
    }
}
