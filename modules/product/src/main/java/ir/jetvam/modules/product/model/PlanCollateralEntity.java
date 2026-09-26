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
        uniqueConstraints = @UniqueConstraint(name = "uk_product_plan_collateral_code", columnNames = {"plan_id", "code"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanCollateralEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "minimum_coverage_percent", nullable = false, precision = 7, scale = 2)
    private BigDecimal minimumCoveragePercent;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "configuration_json", nullable = false, columnDefinition = "text")
    private String configurationJson;

    public PlanCollateralEntity(
            PlanEntity plan,
            String code,
            String title,
            BigDecimal minimumCoveragePercent,
            boolean required,
            boolean enabled,
            String configurationJson
    ) {
        this.plan = Preconditions.requireNonNull(plan, "plan");
        this.code = normalize(code);
        this.title = Preconditions.requireText(title, "title").strip();
        this.minimumCoveragePercent = requireNonNegative(minimumCoveragePercent, "minimumCoveragePercent");
        this.required = required;
        this.enabled = enabled;
        this.configurationJson = jsonOrEmpty(configurationJson);
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String name) {
        Preconditions.requireNonNull(value, name);
        Preconditions.require(value.signum() >= 0, name + " must not be negative");
        return value;
    }

    private static String normalize(String value) {
        return Preconditions.requireText(value, "code").strip().toUpperCase();
    }

    private static String jsonOrEmpty(String value) {
        return value == null || value.isBlank() ? "{}" : value.strip();
    }
}
