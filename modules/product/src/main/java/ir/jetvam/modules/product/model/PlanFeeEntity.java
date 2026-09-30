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
        uniqueConstraints = @UniqueConstraint(name = "uk_product_plan_fee_definition", columnNames = {"plan_id", "fee_definition_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanFeeEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fee_definition_id", nullable = false)
    private FeeDefinitionEntity feeDefinition;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public PlanFeeEntity(
            PlanEntity plan,
            FeeDefinitionEntity feeDefinition,
            boolean enabled
    ) {
        this.plan = Preconditions.requireNonNull(plan, "plan");
        this.feeDefinition = Preconditions.requireNonNull(feeDefinition, "feeDefinition");
        reconfigure(enabled);
    }

    public void reconfigure(boolean enabled) {
        this.enabled = enabled;
    }
}
