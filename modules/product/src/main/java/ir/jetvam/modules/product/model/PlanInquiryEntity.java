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
 * Configures one ordered inquiry that must run for a plan at a named journey stage.
 * String codes keep the product model independent from assessment implementations and providers.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Entity
@Table(
        name = "product_plan_inquiry",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_plan_inquiry_code", columnNames = {"plan_id", "code"}),
                @UniqueConstraint(name = "uk_product_plan_inquiry_sequence", columnNames = {"plan_id", "sequence_number"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanInquiryEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "stage_code", nullable = false, length = 100)
    private String stageCode;

    @Column(name = "sequence_number", nullable = false)
    private int sequence;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "configuration_json", nullable = false, columnDefinition = "text")
    private String configurationJson;

    public PlanInquiryEntity(
            PlanEntity plan,
            String code,
            String title,
            String stageCode,
            int sequence,
            boolean required,
            boolean enabled,
            String configurationJson
    ) {
        this.plan = Preconditions.requireNonNull(plan, "plan");
        this.code = normalize(code, "code");
        this.title = Preconditions.requireText(title, "title").strip();
        this.stageCode = normalize(stageCode, "stageCode");
        this.sequence = Preconditions.requirePositive(sequence, "sequence");
        this.required = required;
        this.enabled = enabled;
        this.configurationJson = jsonOrEmpty(configurationJson);
    }

    private static String normalize(String value, String name) {
        return Preconditions.requireText(value, name).strip().toUpperCase();
    }

    private static String jsonOrEmpty(String value) {
        return value == null || value.isBlank() ? "{}" : value.strip();
    }
}
