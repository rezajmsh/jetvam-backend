package ir.jetvam.modules.product.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Stores one plan-specific numeric value for a centrally declared control parameter.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(name = "product_plan_control_parameter_value")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanControlParameterValueEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_control_id", nullable = false)
    private PlanControlEntity planControl;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "parameter_definition_id", nullable = false)
    private ControlParameterDefinitionEntity parameterDefinition;
    @Column(name = "numeric_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal numericValue;

    public PlanControlParameterValueEntity(
            PlanControlEntity planControl,
            ControlParameterDefinitionEntity parameterDefinition,
            BigDecimal numericValue
    ) {
        this.planControl = Preconditions.requireNonNull(planControl, "planControl");
        this.parameterDefinition = Preconditions.requireNonNull(parameterDefinition, "parameterDefinition");
        update(numericValue);
    }

    public void update(BigDecimal numericValue) {
        this.numericValue = Preconditions.requireNonNull(numericValue, "numericValue");
    }
}
