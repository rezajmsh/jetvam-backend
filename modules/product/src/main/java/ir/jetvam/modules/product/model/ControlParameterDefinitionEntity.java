package ir.jetvam.modules.product.model;

import ir.jetvam.infra.persistence.entity.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Describes one typed parameter accepted by a reusable control definition.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(name = "product_control_parameter_definition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ControlParameterDefinitionEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "control_definition_id", nullable = false)
    private ControlDefinitionEntity controlDefinition;
    @Column(name = "code", nullable = false, length = 100)
    private String code;
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(name = "value_role", nullable = false, length = 20)
    private ControlParameterRole valueRole;
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private ControlParameterDataType dataType;
    @Column(name = "required", nullable = false)
    private boolean required;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
