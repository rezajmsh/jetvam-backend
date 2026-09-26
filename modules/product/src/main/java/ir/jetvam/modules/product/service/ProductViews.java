package ir.jetvam.modules.product.service;

import ir.jetvam.modules.product.model.PublicationStatus;
import ir.jetvam.modules.product.model.PlanControlType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Groups immutable product catalog projections returned across application boundaries.
 * The views expose configuration without leaking JPA entities or provider details.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class ProductViews {

    private ProductViews() {
    }

    public record Product(
            UUID id,
            String code,
            String name,
            String description,
            PublicationStatus status,
            long version,
            List<Plan> plans
    ) {
    }

    public record Plan(
            UUID id,
            UUID productId,
            String code,
            String name,
            String description,
            BigDecimal minimumAmount,
            BigDecimal maximumAmount,
            int minimumTermMonths,
            int maximumTermMonths,
            BigDecimal annualInterestRate,
            PublicationStatus status,
            long version,
            List<Inquiry> inquiries,
            List<Guarantee> guarantees,
            List<Collateral> collaterals,
            List<Fee> fees,
            List<Control> controls
    ) {
    }

    public record Inquiry(
            String code,
            String title,
            String stageCode,
            int sequence,
            boolean required,
            boolean enabled,
            String configurationJson
    ) {
    }

    public record Guarantee(
            String code,
            String title,
            int minimumCount,
            int maximumCount,
            boolean required,
            boolean enabled,
            String configurationJson
    ) {
    }

    public record Collateral(
            String code,
            String title,
            BigDecimal minimumCoveragePercent,
            boolean required,
            boolean enabled,
            String configurationJson
    ) {
    }

    public record Fee(
            String code,
            String title,
            BigDecimal amount,
            String currency,
            String triggerCode,
            String sourceInquiryCode,
            boolean refundable,
            boolean enabled
    ) {
    }

    public record Control(
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
    }
}
