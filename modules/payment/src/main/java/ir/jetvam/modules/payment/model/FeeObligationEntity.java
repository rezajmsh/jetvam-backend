package ir.jetvam.modules.payment.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Stores an immutable payable fee linked to a business reference and customer.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Entity
@Table(
        name = "payment_fee_obligation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_fee_reference_code",
                columnNames = {"reference_type", "reference_id", "fee_code"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeeObligationEntity extends AbstractAuditableUuidEntity {

    @Column(name = "customer_party_id", nullable = false)
    private UUID customerPartyId;

    @Column(name = "reference_type", nullable = false, length = 50)
    private String referenceType;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(name = "fee_code", nullable = false, length = 100)
    private String feeCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private FeeCategory category;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "activation_key", length = 100)
    private String activationKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private FeeStatus status;

    @Column(name = "paid_at")
    private Instant paidAt;

    public FeeObligationEntity(
            UUID customerPartyId,
            String referenceType,
            UUID referenceId,
            String feeCode,
            String title,
            FeeCategory category,
            BigDecimal amount,
            String currency,
            String activationKey
    ) {
        this.customerPartyId = Preconditions.requireNonNull(customerPartyId, "customerPartyId");
        this.referenceType = normalize(referenceType, "referenceType");
        this.referenceId = Preconditions.requireNonNull(referenceId, "referenceId");
        this.feeCode = normalize(feeCode, "feeCode");
        this.title = Preconditions.requireText(title, "title").strip();
        this.category = Preconditions.requireNonNull(category, "category");
        this.amount = Preconditions.requireNonNull(amount, "amount");
        Preconditions.require(amount.signum() >= 0, "amount must not be negative");
        this.currency = normalize(currency, "currency");
        Preconditions.require(this.currency.length() == 3, "currency must contain three characters");
        this.activationKey = activationKey == null || activationKey.isBlank()
                ? null : normalize(activationKey, "activationKey");
        this.status = amount.signum() == 0
                ? FeeStatus.PAID
                : this.activationKey == null ? FeeStatus.PENDING : FeeStatus.BLOCKED;
    }

    public void activate(String key) {
        if (status == FeeStatus.BLOCKED && activationKey.equals(normalize(key, "activationKey"))) {
            status = FeeStatus.PENDING;
        }
    }

    public void cancelUnpaid() {
        if (status == FeeStatus.BLOCKED || status == FeeStatus.PENDING) {
            status = FeeStatus.CANCELLED;
        }
    }

    public void markPaid(Instant at) {
        if (status == FeeStatus.PAID) {
            return;
        }
        Preconditions.require(status == FeeStatus.PENDING, "Only pending fees can be paid");
        this.status = FeeStatus.PAID;
        this.paidAt = Preconditions.requireNonNull(at, "at");
    }

    private static String normalize(String value, String name) {
        return Preconditions.requireText(value, name).strip().toUpperCase();
    }
}
