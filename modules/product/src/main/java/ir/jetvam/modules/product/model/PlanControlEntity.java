package ir.jetvam.modules.product.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Declares one eligibility policy attached to a plan and its optional inquiry dependency.
 * Thresholds are stored generically while type-specific invariants are enforced at construction.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Entity
@Table(
        name = "product_plan_control",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_plan_control_code", columnNames = {"plan_id", "code"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanControlEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_type", nullable = false, length = 40)
    private PlanControlType type;

    @Column(name = "minimum_value", precision = 19, scale = 4)
    private BigDecimal minimumValue;

    @Column(name = "maximum_value", precision = 19, scale = 4)
    private BigDecimal maximumValue;

    @Column(name = "source_inquiry_code", length = 100)
    private String sourceInquiryCode;

    @Column(name = "failure_message", nullable = false, length = 500)
    private String failureMessage;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public PlanControlEntity(
            PlanEntity plan,
            String code,
            String title,
            int priority,
            PlanControlType type,
            BigDecimal minimumValue,
            BigDecimal maximumValue,
            String sourceInquiryCode,
            String failureMessage,
            boolean enabled
    ) {
        this.plan = Preconditions.requireNonNull(plan, "plan");
        this.code = normalize(code, "code");
        this.title = Preconditions.requireText(title, "title").strip();
        this.priority = Preconditions.requirePositive(priority, "priority");
        this.type = Preconditions.requireNonNull(type, "type");
        this.minimumValue = nonNegativeOrNull(minimumValue, "minimumValue");
        this.maximumValue = nonNegativeOrNull(maximumValue, "maximumValue");
        this.sourceInquiryCode = sourceInquiryCode == null || sourceInquiryCode.isBlank()
                ? null : normalize(sourceInquiryCode, "sourceInquiryCode");
        this.failureMessage = Preconditions.requireText(failureMessage, "failureMessage").strip();
        this.enabled = enabled;
        validateTypeConfiguration();
    }

    private void validateTypeConfiguration() {
        switch (type) {
            case AGE_RANGE -> {
                Preconditions.require(minimumValue != null || maximumValue != null,
                        "AGE_RANGE requires minimumValue or maximumValue");
                if (minimumValue != null && maximumValue != null) {
                    Preconditions.require(maximumValue.compareTo(minimumValue) >= 0,
                            "AGE_RANGE maximumValue must not be less than minimumValue");
                }
                Preconditions.require(sourceInquiryCode == null, "AGE_RANGE must not reference an inquiry");
            }
            case MINIMUM_CREDIT_RANK -> {
                Preconditions.require(minimumValue != null, "MINIMUM_CREDIT_RANK requires minimumValue");
                Preconditions.require(isWholeNumber(minimumValue),
                        "MINIMUM_CREDIT_RANK minimumValue must be a whole number");
                Preconditions.require(sourceInquiryCode != null,
                        "MINIMUM_CREDIT_RANK requires sourceInquiryCode");
                Preconditions.require(maximumValue == null,
                        "MINIMUM_CREDIT_RANK does not support maximumValue");
            }
            case NO_BAD_CHEQUE -> {
                Preconditions.require(sourceInquiryCode != null, "NO_BAD_CHEQUE requires sourceInquiryCode");
                Preconditions.require(minimumValue == null && maximumValue == null,
                        "NO_BAD_CHEQUE does not support thresholds");
            }
        }
    }

    private static BigDecimal nonNegativeOrNull(BigDecimal value, String name) {
        if (value != null) {
            Preconditions.require(value.signum() >= 0, name + " must not be negative");
        }
        return value;
    }

    private static boolean isWholeNumber(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= 0;
    }

    private static String normalize(String value, String name) {
        return Preconditions.requireText(value, name).strip().toUpperCase();
    }
}
