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
 * Configures a plan's guarantor or other personal-guarantee requirement.
 * The rule code and JSON configuration avoid coupling Product to Guarantee workflows.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Entity
@Table(
        name = "product_plan_guarantee",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_plan_guarantee_code", columnNames = {"plan_id", "code"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanGuaranteeEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "minimum_count", nullable = false)
    private int minimumCount;

    @Column(name = "maximum_count", nullable = false)
    private int maximumCount;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "configuration_json", nullable = false, columnDefinition = "text")
    private String configurationJson;

    public PlanGuaranteeEntity(
            PlanEntity plan,
            String code,
            String title,
            int minimumCount,
            int maximumCount,
            boolean required,
            boolean enabled,
            String configurationJson
    ) {
        this.plan = Preconditions.requireNonNull(plan, "plan");
        this.code = normalize(code);
        this.title = Preconditions.requireText(title, "title").strip();
        this.minimumCount = Preconditions.requireNonNegative(minimumCount, "minimumCount");
        this.maximumCount = Preconditions.requireNonNegative(maximumCount, "maximumCount");
        Preconditions.require(maximumCount >= minimumCount, "maximumCount must not be less than minimumCount");
        Preconditions.require(!required || minimumCount > 0, "A required guarantee must have a positive minimumCount");
        this.required = required;
        this.enabled = enabled;
        this.configurationJson = jsonOrEmpty(configurationJson);
    }

    private static String normalize(String value) {
        return Preconditions.requireText(value, "code").strip().toUpperCase();
    }

    private static String jsonOrEmpty(String value) {
        return value == null || value.isBlank() ? "{}" : value.strip();
    }
}
