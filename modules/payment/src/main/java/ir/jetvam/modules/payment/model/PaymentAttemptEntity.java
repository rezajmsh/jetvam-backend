package ir.jetvam.modules.payment.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Records an idempotent attempt to pay one fee through an external or operational channel.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Entity
@Table(name = "payment_attempt")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttemptEntity extends AbstractAuditableUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fee_obligation_id", nullable = false)
    private FeeObligationEntity feeObligation;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentAttemptStatus status;

    @Column(name = "provider_reference", length = 150)
    private String providerReference;

    @Column(name = "gateway_code", length = 100)
    private String gatewayCode;

    @Column(name = "checkout_token_hash", length = 64)
    private String checkoutTokenHash;

    @Column(name = "return_url", length = 1000)
    private String returnUrl;

    @Column(name = "redirect_url", length = 1500)
    private String redirectUrl;

    @Column(name = "completed_at")
    private Instant completedAt;

    public PaymentAttemptEntity(FeeObligationEntity feeObligation, String idempotencyKey) {
        this.feeObligation = Preconditions.requireNonNull(feeObligation, "feeObligation");
        this.idempotencyKey = Preconditions.requireText(idempotencyKey, "idempotencyKey").strip();
        this.status = PaymentAttemptStatus.CREATED;
    }

    public void complete(boolean successful, String providerReference, Instant at) {
        if (status != PaymentAttemptStatus.CREATED) {
            return;
        }
        this.providerReference = providerReference == null ? null : providerReference.strip();
        this.completedAt = Preconditions.requireNonNull(at, "at");
        this.status = successful ? PaymentAttemptStatus.SUCCEEDED : PaymentAttemptStatus.FAILED;
        if (successful) {
            feeObligation.markPaid(at);
        }
    }

    public void prepareCheckout(
            String gatewayCode,
            String checkoutTokenHash,
            String returnUrl,
            String redirectUrl
    ) {
        if (this.gatewayCode != null) {
            return;
        }
        this.gatewayCode = Preconditions.requireText(gatewayCode, "gatewayCode").strip().toUpperCase();
        this.checkoutTokenHash = Preconditions.requireText(checkoutTokenHash, "checkoutTokenHash").strip();
        this.returnUrl = Preconditions.requireText(returnUrl, "returnUrl").strip();
        this.redirectUrl = Preconditions.requireText(redirectUrl, "redirectUrl").strip();
    }
}
