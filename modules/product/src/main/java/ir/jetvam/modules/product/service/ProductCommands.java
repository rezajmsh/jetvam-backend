package ir.jetvam.modules.product.service;

import ir.jetvam.modules.product.model.PublicationStatus;
import ir.jetvam.modules.product.model.PlanControlType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Groups the write contracts accepted by the product application boundary.
 * Nested records keep the public surface cohesive without coupling it to HTTP request types.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class ProductCommands {

    private ProductCommands() {
    }

    public record CreateProduct(String code, String name, String description) {
    }

    public record ReviseProduct(String name, String description) {
    }

    public record ChangeStatus(PublicationStatus status) {
    }

    public record CreatePlan(
            String code,
            String name,
            String description,
            BigDecimal minimumAmount,
            BigDecimal maximumAmount,
            int minimumTermMonths,
            int maximumTermMonths,
            BigDecimal annualInterestRate
    ) {
    }

    public record RevisePlan(
            String name,
            String description,
            BigDecimal minimumAmount,
            BigDecimal maximumAmount,
            int minimumTermMonths,
            int maximumTermMonths,
            BigDecimal annualInterestRate
    ) {
    }

    public record ConfigurePlan(
            List<InquiryRule> inquiries,
            List<GuaranteeRule> guarantees,
            List<CollateralRule> collaterals,
            List<FeeRule> fees,
            List<ControlRule> controls
    ) {
        public ConfigurePlan {
            inquiries = inquiries == null ? List.of() : List.copyOf(inquiries);
            guarantees = guarantees == null ? List.of() : List.copyOf(guarantees);
            collaterals = collaterals == null ? List.of() : List.copyOf(collaterals);
            fees = fees == null ? List.of() : List.copyOf(fees);
            controls = controls == null ? List.of() : List.copyOf(controls);
        }
    }

    public record InquiryRule(
            String code,
            String title,
            String stageCode,
            int sequence,
            boolean required,
            boolean enabled,
            String configurationJson
    ) {
    }

    public record GuaranteeRule(
            String code,
            String title,
            int minimumCount,
            int maximumCount,
            boolean required,
            boolean enabled,
            String configurationJson
    ) {
    }

    public record CollateralRule(
            String code,
            String title,
            BigDecimal minimumCoveragePercent,
            boolean required,
            boolean enabled,
            String configurationJson
    ) {
    }

    public record FeeRule(
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

    public record ControlRule(
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
