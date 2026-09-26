package ir.jetvam.modules.payment.service;

import ir.jetvam.common.exception.OperationNotAllowedException;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.payment.model.FeeCategory;
import ir.jetvam.modules.payment.model.FeeObligationEntity;
import ir.jetvam.modules.payment.model.FeeStatus;
import ir.jetvam.modules.payment.model.PaymentAttemptEntity;
import ir.jetvam.modules.payment.repository.FeeObligationRepository;
import ir.jetvam.modules.payment.repository.PaymentAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implements fee obligations and idempotent payment confirmation without trusting customer callbacks.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultPaymentService implements PaymentService {

    private final FeeObligationRepository feeRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final TimeProvider timeProvider;

    @Override
    @Transactional
    public void createObligations(
            UUID customerPartyId,
            UUID referenceId,
            List<PaymentModels.FeeDefinition> fees
    ) {
        Preconditions.requireNonNull(customerPartyId, "customerPartyId");
        Preconditions.requireNonNull(referenceId, "referenceId");
        Preconditions.requireNonNull(fees, "fees").forEach(fee -> {
            String code = Preconditions.requireText(fee.code(), "fee.code").strip().toUpperCase();
            if (!feeRepository.existsByReferenceTypeAndReferenceIdAndFeeCode(
                    PaymentModels.LOAN_APPLICATION, referenceId, code
            )) {
                feeRepository.save(new FeeObligationEntity(
                        customerPartyId, PaymentModels.LOAN_APPLICATION, referenceId, code, fee.title(),
                        fee.category(), fee.amount(), fee.currency(), fee.activationKey()
                ));
            }
        });
    }

    @Override
    @Transactional
    public void activate(UUID referenceId, String activationKey) {
        String key = Preconditions.requireText(activationKey, "activationKey").strip().toUpperCase();
        feeRepository.findAllByReferenceTypeAndReferenceIdOrderByFeeCode(
                PaymentModels.LOAN_APPLICATION,
                Preconditions.requireNonNull(referenceId, "referenceId")
        ).forEach(fee -> fee.activate(key));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean allPaid(UUID referenceId, String activationKey) {
        return !feeRepository.existsByReferenceTypeAndReferenceIdAndActivationKeyAndStatusNot(
                PaymentModels.LOAN_APPLICATION,
                Preconditions.requireNonNull(referenceId, "referenceId"),
                Preconditions.requireText(activationKey, "activationKey").strip().toUpperCase(),
                FeeStatus.PAID
        );
    }

    @Override
    @Transactional
    public void cancelUnpaid(UUID referenceId) {
        feeRepository.findAllByReferenceTypeAndReferenceIdOrderByFeeCode(
                PaymentModels.LOAN_APPLICATION,
                Preconditions.requireNonNull(referenceId, "referenceId")
        ).forEach(FeeObligationEntity::cancelUnpaid);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentModels.FeeView> findFees(UUID customerPartyId, UUID referenceId) {
        return feeRepository.findAllByReferenceTypeAndReferenceIdOrderByFeeCode(
                        PaymentModels.LOAN_APPLICATION,
                        Preconditions.requireNonNull(referenceId, "referenceId")
                ).stream()
                .peek(fee -> requireOwner(fee, customerPartyId))
                .map(DefaultPaymentService::toView)
                .toList();
    }

    @Override
    @Transactional
    public PaymentModels.AttemptView initiate(UUID customerPartyId, UUID feeId, String idempotencyKey) {
        String key = Preconditions.requireText(idempotencyKey, "idempotencyKey").strip();
        return attemptRepository.findByIdempotencyKey(key)
                .map(attempt -> {
                    requireOwner(attempt.getFeeObligation(), customerPartyId);
                    if (!attempt.getFeeObligation().getId().equals(feeId)) {
                        throw new OperationNotAllowedException(
                                "initiate-payment",
                                "Idempotency key belongs to another fee"
                        );
                    }
                    return toView(attempt);
                })
                .orElseGet(() -> {
                    FeeObligationEntity fee = feeRepository.findById(Preconditions.requireNonNull(feeId, "feeId"))
                            .orElseThrow(() -> new ResourceNotFoundException("fee", feeId));
                    requireOwner(fee, customerPartyId);
                    if (fee.getStatus() != FeeStatus.PENDING) {
                        throw new OperationNotAllowedException("initiate-payment", "Fee is not pending");
                    }
                    return toView(attemptRepository.save(new PaymentAttemptEntity(fee, key)));
                });
    }

    @Override
    @Transactional
    public PaymentModels.AttemptView confirm(UUID attemptId, boolean successful, String providerReference) {
        PaymentAttemptEntity attempt = attemptRepository.findById(Preconditions.requireNonNull(attemptId, "attemptId"))
                .orElseThrow(() -> new ResourceNotFoundException("paymentAttempt", attemptId));
        attempt.complete(successful, providerReference, timeProvider.now());
        return toView(attempt);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean allPaid(UUID referenceId, FeeCategory category) {
        return !feeRepository.existsByReferenceTypeAndReferenceIdAndCategoryAndStatusNot(
                PaymentModels.LOAN_APPLICATION,
                Preconditions.requireNonNull(referenceId, "referenceId"),
                Preconditions.requireNonNull(category, "category"),
                FeeStatus.PAID
        );
    }

    private static void requireOwner(FeeObligationEntity fee, UUID customerPartyId) {
        if (!fee.getCustomerPartyId().equals(customerPartyId)) {
            throw new OperationNotAllowedException("access-payment", "Payment does not belong to this customer");
        }
    }

    private static PaymentModels.FeeView toView(FeeObligationEntity fee) {
        return new PaymentModels.FeeView(
                fee.getId(), fee.getReferenceId(), fee.getFeeCode(), fee.getTitle(), fee.getCategory(),
                fee.getAmount(), fee.getCurrency(), fee.getActivationKey(), fee.getStatus(), fee.getPaidAt()
        );
    }

    private static PaymentModels.AttemptView toView(PaymentAttemptEntity attempt) {
        return new PaymentModels.AttemptView(
                attempt.getId(), attempt.getFeeObligation().getId(), attempt.getStatus(),
                attempt.getProviderReference()
        );
    }
}
