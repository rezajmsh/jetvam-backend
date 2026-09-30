package ir.jetvam.modules.product.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import ir.jetvam.infra.persistence.entity.converter.InquiryTypeConverter;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Defines a reusable eligibility control, its evaluator and optional inquiry dependency.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(name = "product_control_definition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ControlDefinitionEntity extends AbstractAuditableUuidEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(name = "evaluator_type", nullable = false, length = 50)
    private PlanControlType evaluatorType;
    @Column(name = "inquiry_code", length = 100)
    @Convert(converter = InquiryTypeConverter.class)
    private InquiryType inquiryCode;
    @Column(name = "default_failure_message", nullable = false, length = 500)
    private String defaultFailureMessage;
    @Column(name = "active", nullable = false)
    private boolean active;
    @OneToMany(mappedBy = "controlDefinition", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<ControlParameterDefinitionEntity> parameters = new LinkedHashSet<>();

    public void update(String title, String defaultFailureMessage, boolean active) {
        this.title = Preconditions.requireText(title, "title").strip();
        this.defaultFailureMessage = Preconditions.requireText(defaultFailureMessage, "defaultFailureMessage").strip();
        this.active = active;
    }
}
