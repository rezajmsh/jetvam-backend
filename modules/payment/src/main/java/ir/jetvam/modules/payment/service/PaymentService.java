package ir.jetvam.modules.payment.service;

import ir.jetvam.modules.payment.model.FeeCategory;

import java.util.List;
import java.util.UUID;

/**
 * Defines fee creation, customer checkout and trusted payment confirmation operations.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface PaymentService {

    void createObligations(UUID customerPartyId, UUID referenceId, List<PaymentModels.FeeDefinition> fees);

    void activate(UUID referenceId, String activationKey);

    boolean allPaid(UUID referenceId, String activationKey);

    void cancelUnpaid(UUID referenceId);

    List<PaymentModels.FeeView> findFees(UUID customerPartyId, UUID referenceId);

    PaymentModels.AttemptView initiate(UUID customerPartyId, UUID feeId, String idempotencyKey);

    PaymentModels.AttemptView confirm(UUID attemptId, boolean successful, String providerReference);

    boolean allPaid(UUID referenceId, FeeCategory category);
}
