package ir.jetvam.modules.product.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import ir.jetvam.infra.persistence.entity.converter.InquiryTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Defines one reusable commercial fee independently from plans and payment attempts.
 * A plan association selects the fee while Payment owns the resulting obligation.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(name = "product_fee_definition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeeDefinitionEntity extends AbstractAuditableUuidEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ProductFeeCategory category;
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    @Column(name = "trigger_code", nullable = false, length = 100)
    private String triggerCode;
    @Column(name = "source_inquiry_code", length = 100)
    @Convert(converter = InquiryTypeConverter.class)
    private InquiryType sourceInquiryCode;
    @Column(name = "refundable", nullable = false)
    private boolean refundable;
    @Column(name = "active", nullable = false)
    private boolean active;

    public FeeDefinitionEntity(
            String code, String title, ProductFeeCategory category, BigDecimal amount, String currency,
            String triggerCode, InquiryType sourceInquiryCode, boolean refundable, boolean active
    ) {
        this.code = normalize(code, "code");
        update(title, category, amount, currency, triggerCode, sourceInquiryCode, refundable, active);
    }

    public void update(
            String title, ProductFeeCategory category, BigDecimal amount, String currency,
            String triggerCode, InquiryType sourceInquiryCode, boolean refundable, boolean active
    ) {
        this.title = Preconditions.requireText(title, "title").strip();
        this.category = Preconditions.requireNonNull(category, "category");
        this.amount = Preconditions.requireNonNull(amount, "amount");
        Preconditions.require(amount.signum() >= 0, "amount must not be negative");
        this.currency = normalize(currency, "currency");
        Preconditions.require(this.currency.length() == 3, "currency must contain three characters");
        this.triggerCode = normalize(triggerCode, "triggerCode");
        this.sourceInquiryCode = sourceInquiryCode;
        Preconditions.require(category != ProductFeeCategory.INQUIRY || this.sourceInquiryCode != null,
                "Inquiry fees must reference an inquiry");
        this.refundable = refundable;
        this.active = active;
    }

    private static String normalize(String value, String field) {
        return Preconditions.requireText(value, field).strip().toUpperCase();
    }
}
