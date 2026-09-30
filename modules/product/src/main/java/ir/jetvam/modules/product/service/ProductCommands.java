package ir.jetvam.modules.product.service;

import ir.jetvam.modules.product.model.PublicationStatus;
import ir.jetvam.modules.product.model.ControlSubjectType;

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
            GuarantorPolicy guarantorPolicy,
            List<CollateralRule> guarantorCollaterals,
            List<CollateralRule> collaterals,
            List<FeeRule> fees,
            List<ControlRule> controls
    ) {
        public ConfigurePlan {
            guarantorCollaterals = guarantorCollaterals == null ? List.of() : List.copyOf(guarantorCollaterals);
            collaterals = collaterals == null ? List.of() : List.copyOf(collaterals);
            fees = fees == null ? List.of() : List.copyOf(fees);
            controls = controls == null ? List.of() : List.copyOf(controls);
        }
    }

    public record GuarantorPolicy(
            int minimumCount,
            int maximumCount,
            boolean required,
            boolean requiresCollateral,
            boolean enabled
    ) {
    }

    public record CollateralRule(
            java.util.UUID collateralTypeId,
            BigDecimal minimumCoveragePercent,
            boolean required,
            boolean enabled
    ) {
    }

    public record FeeRule(
            java.util.UUID feeDefinitionId,
            boolean enabled
    ) {
    }

    public record ControlRule(
            java.util.UUID controlDefinitionId,
            ControlSubjectType subjectType,
            int priority,
            List<ControlParameterValue> parameters,
            boolean enabled
    ) {
        public ControlRule {
            parameters = parameters == null ? List.of() : List.copyOf(parameters);
        }
    }

    public record ControlParameterValue(java.util.UUID parameterDefinitionId, BigDecimal numericValue) {
    }
}
