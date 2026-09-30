package ir.jetvam.modules.product.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Declares one eligibility policy attached to a plan and its optional inquiry dependency.
 * Thresholds are stored generically while type-specific invariants are enforced at construction.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Entity
@Table(
        name = "product_plan_control",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_plan_control_definition",
                columnNames = {"plan_id", "subject_type", "control_definition_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanControlEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "control_definition_id", nullable = false)
    private ControlDefinitionEntity controlDefinition;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    private ControlSubjectType subjectType;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @OneToMany(mappedBy = "planControl", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<PlanControlParameterValueEntity> parameterValues = new LinkedHashSet<>();

    public PlanControlEntity(
            PlanEntity plan,
            ControlDefinitionEntity controlDefinition,
            ControlSubjectType subjectType,
            int priority,
            boolean enabled,
            Collection<ParameterValue> values
    ) {
        this.plan = Preconditions.requireNonNull(plan, "plan");
        this.controlDefinition = Preconditions.requireNonNull(controlDefinition, "controlDefinition");
        this.subjectType = Preconditions.requireNonNull(subjectType, "subjectType");
        reconfigure(priority, enabled, values);
    }

    public BigDecimal value(ControlParameterRole role) {
        return parameterValues.stream()
                .filter(value -> value.getParameterDefinition().getValueRole() == role)
                .map(PlanControlParameterValueEntity::getNumericValue)
                .findFirst().orElse(null);
    }

    public void reconfigure(int priority, boolean enabled, Collection<ParameterValue> values) {
        Collection<ParameterValue> safeValues = Preconditions.requireNonNull(values, "values");
        Set<java.util.UUID> supplied = new java.util.HashSet<>();
        java.util.Map<java.util.UUID, ResolvedParameter> resolved = new java.util.LinkedHashMap<>();
        safeValues.forEach(value -> {
            ControlParameterDefinitionEntity definition = controlDefinition.getParameters().stream()
                    .filter(item -> item.getId().equals(value.parameterDefinitionId()))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException(
                            "Parameter does not belong to selected control: " + value.parameterDefinitionId()));
            Preconditions.require(supplied.add(definition.getId()), "Duplicate control parameter");
            Preconditions.require(value.numericValue() != null && value.numericValue().signum() >= 0,
                    "Control parameter value must not be negative");
            if (definition.getDataType() == ControlParameterDataType.INTEGER) {
                Preconditions.require(isWholeNumber(value.numericValue()), "Control parameter must be an integer");
            }
            resolved.put(definition.getId(), new ResolvedParameter(definition, value.numericValue()));
        });
        controlDefinition.getParameters().stream().filter(ControlParameterDefinitionEntity::isRequired)
                .forEach(definition -> Preconditions.require(supplied.contains(definition.getId()),
                        "Required control parameter is missing: " + definition.getCode()));
        BigDecimal minimum = resolved.values().stream()
                .filter(item -> item.definition().getValueRole() == ControlParameterRole.MINIMUM)
                .map(ResolvedParameter::value).findFirst().orElse(null);
        BigDecimal maximum = resolved.values().stream()
                .filter(item -> item.definition().getValueRole() == ControlParameterRole.MAXIMUM)
                .map(ResolvedParameter::value).findFirst().orElse(null);
        if (minimum != null && maximum != null) {
            Preconditions.require(maximum.compareTo(minimum) >= 0,
                    "Maximum control parameter must not be less than minimum");
        }
        Preconditions.require(!controlDefinition.getParameters().isEmpty() || safeValues.isEmpty(),
                "This control does not accept parameters");
        this.priority = Preconditions.requirePositive(priority, "priority");
        this.enabled = enabled;
        parameterValues.removeIf(item -> !resolved.containsKey(item.getParameterDefinition().getId()));
        resolved.forEach((definitionId, parameter) -> parameterValues.stream()
                .filter(item -> item.getParameterDefinition().getId().equals(definitionId))
                .findFirst()
                .ifPresentOrElse(
                        item -> item.update(parameter.value()),
                        () -> parameterValues.add(new PlanControlParameterValueEntity(
                                this, parameter.definition(), parameter.value()
                        ))
                ));
    }

    private static boolean isWholeNumber(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= 0;
    }

    public record ParameterValue(java.util.UUID parameterDefinitionId, BigDecimal numericValue) {
    }

    private record ResolvedParameter(ControlParameterDefinitionEntity definition, BigDecimal value) {
    }
}
