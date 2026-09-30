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
 * Associates a master collateral type with the collateral expected from each guarantor of a plan.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(
        name = "product_plan_guarantor_collateral",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_guarantor_collateral_type",
                columnNames = {"plan_id", "collateral_type_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanGuarantorCollateralEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collateral_type_id", nullable = false)
    private CollateralTypeEntity collateralType;
    @Column(name = "minimum_coverage_percent", nullable = false, precision = 7, scale = 2)
    private BigDecimal minimumCoveragePercent;
    @Column(name = "required", nullable = false)
    private boolean required;
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public PlanGuarantorCollateralEntity(
            PlanEntity plan, CollateralTypeEntity collateralType, BigDecimal minimumCoveragePercent,
            boolean required, boolean enabled
    ) {
        this.plan = Preconditions.requireNonNull(plan, "plan");
        this.collateralType = Preconditions.requireNonNull(collateralType, "collateralType");
        reconfigure(minimumCoveragePercent, required, enabled);
    }

    public void reconfigure(BigDecimal minimumCoveragePercent, boolean required, boolean enabled) {
        this.minimumCoveragePercent = Preconditions.requireNonNull(minimumCoveragePercent, "minimumCoveragePercent");
        Preconditions.require(minimumCoveragePercent.signum() >= 0, "minimumCoveragePercent must not be negative");
        this.required = required;
        this.enabled = enabled;
    }
}
