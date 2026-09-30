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
 * Configures a plan's collateral or instrument requirement, such as a Sayad cheque.
 * Product stores declarative rules while collateral fulfillment belongs to downstream modules.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Entity
@Table(
        name = "product_plan_collateral",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_plan_collateral_type", columnNames = {"plan_id", "collateral_type_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanCollateralEntity extends AbstractUuidEntity {

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

    public PlanCollateralEntity(
            PlanEntity plan,
            CollateralTypeEntity collateralType,
            BigDecimal minimumCoveragePercent,
            boolean required,
            boolean enabled
    ) {
        this.plan = Preconditions.requireNonNull(plan, "plan");
        this.collateralType = Preconditions.requireNonNull(collateralType, "collateralType");
        reconfigure(minimumCoveragePercent, required, enabled);
    }

    public void reconfigure(BigDecimal minimumCoveragePercent, boolean required, boolean enabled) {
        this.minimumCoveragePercent = requireNonNegative(minimumCoveragePercent, "minimumCoveragePercent");
        this.required = required;
        this.enabled = enabled;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String name) {
        Preconditions.requireNonNull(value, name);
        Preconditions.require(value.signum() >= 0, name + " must not be negative");
        return value;
    }

}
