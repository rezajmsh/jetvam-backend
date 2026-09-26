package ir.jetvam.modules.product.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Configures a plan charge and optionally links it to an inquiry requirement.
 * Unlinked fees represent intrinsic plan charges such as membership or processing costs.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Entity
@Table(
        name = "product_plan_fee",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_plan_fee_code", columnNames = {"plan_id", "code"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanFeeEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "trigger_code", nullable = false, length = 100)
    private String triggerCode;

    @Column(name = "source_inquiry_code", length = 100)
    private String sourceInquiryCode;

    @Column(name = "refundable", nullable = false)
    private boolean refundable;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public PlanFeeEntity(
            PlanEntity plan,
            String code,
            String title,
            BigDecimal amount,
            String currency,
            String triggerCode,
            String sourceInquiryCode,
            boolean refundable,
            boolean enabled
    ) {
        this.plan = Preconditions.requireNonNull(plan, "plan");
        this.code = normalize(code, "code");
        this.title = Preconditions.requireText(title, "title").strip();
        this.amount = requireNonNegative(amount, "amount");
        this.currency = normalize(currency, "currency");
        Preconditions.require(this.currency.length() == 3, "currency must be a three-letter code");
        this.triggerCode = normalize(triggerCode, "triggerCode");
        this.sourceInquiryCode = sourceInquiryCode == null || sourceInquiryCode.isBlank()
                ? null : normalize(sourceInquiryCode, "sourceInquiryCode");
        this.refundable = refundable;
        this.enabled = enabled;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String name) {
        Preconditions.requireNonNull(value, name);
        Preconditions.require(value.signum() >= 0, name + " must not be negative");
        return value;
    }

    private static String normalize(String value, String name) {
        return Preconditions.requireText(value, name).strip().toUpperCase();
    }
}
