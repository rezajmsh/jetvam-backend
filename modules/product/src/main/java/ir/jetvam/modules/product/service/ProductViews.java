package ir.jetvam.modules.product.service;

import ir.jetvam.modules.product.model.PublicationStatus;
import ir.jetvam.modules.product.model.PlanControlType;
import ir.jetvam.modules.product.model.ControlSubjectType;
import ir.jetvam.modules.product.model.ControlParameterRole;
import ir.jetvam.modules.product.model.ControlParameterDataType;

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
            GuarantorPolicy guarantorPolicy,
            List<Collateral> guarantorCollaterals,
            List<Collateral> collaterals,
            List<Fee> fees,
            List<Control> controls
    ) {
    }

    public record GuarantorPolicy(
            int minimumCount,
            int maximumCount,
            boolean required,
            boolean requiresCollateral,
            boolean enabled
    ) {
    }

    public record Collateral(
            UUID collateralTypeId,
            String code,
            String title,
            String handlerCode,
            boolean requiresPhysicalDelivery,
            BigDecimal minimumCoveragePercent,
            boolean required,
            boolean enabled,
            List<DocumentRequirement> documentRequirements
    ) {
    }

    public record DocumentRequirement(
            UUID documentTypeId, String documentTypeCode, String documentTypeTitle, String allowedContentTypes,
            long maximumSizeBytes, boolean required, int minimumCount, int maximumCount, int displayOrder
    ) {
    }

    public record Fee(
            UUID feeDefinitionId,
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
            UUID controlDefinitionId,
            String code,
            String title,
            int priority,
            PlanControlType type,
            ControlSubjectType subjectType,
            BigDecimal minimumValue,
            BigDecimal maximumValue,
            String sourceInquiryCode,
            String failureMessage,
            List<ControlParameter> parameters,
            boolean enabled
    ) {
    }

    public record ControlParameter(
            UUID parameterDefinitionId, String code, String title, ControlParameterRole valueRole,
            ControlParameterDataType dataType, boolean required, BigDecimal value
    ) {
    }
}
