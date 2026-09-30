package ir.jetvam.modules.payment.service;

import ir.jetvam.common.exception.OperationNotAllowedException;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.payment.model.FeeCategory;
import ir.jetvam.modules.payment.model.FeeObligationEntity;
import ir.jetvam.modules.payment.model.FeeStatus;
import ir.jetvam.modules.payment.model.PaymentAttemptEntity;
import ir.jetvam.modules.payment.model.PaymentGatewayEntity;
import ir.jetvam.modules.payment.repository.FeeObligationRepository;
import ir.jetvam.modules.payment.repository.PaymentAttemptRepository;
import ir.jetvam.modules.payment.repository.PaymentGatewayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

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
    private final PaymentGatewayRepository gatewayRepository;
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
    public PaymentModels.AttemptView initiate(UUID customerPartyId, UUID feeId, String idempotencyKey, String returnUrl) {
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
                    PaymentGatewayEntity gateway = gatewayRepository.findFirstByActiveTrueOrderByGatewayCodeAsc()
                            .orElseThrow(() -> new OperationNotAllowedException("initiate-payment", "No active payment gateway"));
                    PaymentAttemptEntity attempt = attemptRepository.save(new PaymentAttemptEntity(fee, key));
                    String token = UUID.randomUUID().toString();
                    String redirectUrl = "/api/v1/payments/mock/" + attempt.getId() + "?token=" + token;
                    attempt.prepareCheckout(gateway.getGatewayCode(), hash(token), returnUrl, redirectUrl);
                    return toView(attempt);
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

    @Override
    @Transactional
    public PaymentModels.AttemptView completeMock(UUID attemptId, String checkoutToken, boolean successful) {
        PaymentAttemptEntity attempt = attemptRepository.findById(Preconditions.requireNonNull(attemptId, "attemptId"))
                .orElseThrow(() -> new ResourceNotFoundException("paymentAttempt", attemptId));
        Preconditions.require("MOCK".equals(attempt.getGatewayCode()), "Attempt does not belong to mock gateway");
        Preconditions.require(MessageDigest.isEqual(
                hash(checkoutToken).getBytes(StandardCharsets.UTF_8),
                attempt.getCheckoutTokenHash().getBytes(StandardCharsets.UTF_8)
        ), "Invalid payment callback token");
        attempt.complete(successful, "MOCK-" + attemptId, timeProvider.now());
        return toView(attempt);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentModels.GatewayView> findGateways() {
        return gatewayRepository.findAllByOrderByGatewayCodeAsc().stream().map(DefaultPaymentService::toView).toList();
    }

    @Override
    @Transactional
    public PaymentModels.GatewayView updateGateway(
            String gatewayCode,
            String title,
            String configurationJson,
            boolean active
    ) {
        PaymentGatewayEntity gateway = gatewayRepository.findByGatewayCode(
                        Preconditions.requireText(gatewayCode, "gatewayCode").strip().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("paymentGateway", gatewayCode));
        if (active) {
            gatewayRepository.findFirstByActiveTrueOrderByGatewayCodeAsc()
                    .filter(current -> !current.getId().equals(gateway.getId()))
                    .ifPresent(current -> current.update(current.getTitle(), current.getConfigurationJson(), false));
        }
        gateway.update(title, configurationJson, active);
        return toView(gateway);
    }

    private static void requireOwner(FeeObligationEntity fee, UUID customerPartyId) {
        if (!fee.getCustomerPartyId().equals(customerPartyId)) {
            throw new OperationNotAllowedException("access-payment", "Payment does not belong to this customer");
        }
    }

    private static PaymentModels.FeeView toView(FeeObligationEntity fee) {
        return new PaymentModels.FeeView(
                fee.getId(), fee.getReferenceType(), fee.getReferenceId(), fee.getFeeCode(), fee.getTitle(),
                fee.getCategory(), fee.getAmount(), fee.getCurrency(), fee.getActivationKey(), fee.getStatus(),
                fee.getPaidAt(), fee.getCreatedAt()
        );
    }

    private static PaymentModels.AttemptView toView(PaymentAttemptEntity attempt) {
        return new PaymentModels.AttemptView(
                attempt.getId(), attempt.getFeeObligation().getId(), attempt.getStatus(),
                attempt.getProviderReference(), attempt.getGatewayCode(), attempt.getRedirectUrl(), attempt.getReturnUrl()
        );
    }

    private static PaymentModels.GatewayView toView(PaymentGatewayEntity gateway) {
        return new PaymentModels.GatewayView(
                gateway.getId(), gateway.getGatewayCode(), gateway.getTitle(), gateway.getAdapterCode(),
                gateway.getConfigurationJson(), gateway.isActive()
        );
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(Preconditions.requireText(value, "checkoutToken").getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
