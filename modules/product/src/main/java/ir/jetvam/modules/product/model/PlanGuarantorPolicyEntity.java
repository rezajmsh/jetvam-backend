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

/**
 * Configures whether a plan needs guarantors and whether they must provide collateral.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(
        name = "product_plan_guarantor_policy",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_plan_guarantor_policy", columnNames = "plan_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanGuarantorPolicyEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;
    @Column(name = "minimum_count", nullable = false)
    private int minimumCount;
    @Column(name = "maximum_count", nullable = false)
    private int maximumCount;
    @Column(name = "required", nullable = false)
    private boolean required;
    @Column(name = "requires_collateral", nullable = false)
    private boolean requiresCollateral;
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public PlanGuarantorPolicyEntity(
            PlanEntity plan, int minimumCount, int maximumCount, boolean required,
            boolean requiresCollateral, boolean enabled
    ) {
        this.plan = Preconditions.requireNonNull(plan, "plan");
        reconfigure(minimumCount, maximumCount, required, requiresCollateral, enabled);
    }

    public void reconfigure(
            int minimumCount, int maximumCount, boolean required,
            boolean requiresCollateral, boolean enabled
    ) {
        this.minimumCount = Preconditions.requireNonNegative(minimumCount, "minimumCount");
        this.maximumCount = Preconditions.requireNonNegative(maximumCount, "maximumCount");
        Preconditions.require(maximumCount >= minimumCount, "maximumCount must not be less than minimumCount");
        Preconditions.require(!required || minimumCount > 0, "A required guarantor policy must have a positive minimumCount");
        this.required = required;
        this.requiresCollateral = requiresCollateral;
        this.enabled = enabled;
    }
}
