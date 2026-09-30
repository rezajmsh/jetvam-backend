package ir.jetvam.modules.product.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Defines one centrally managed collateral type and the workflow handler responsible for it.
 * Plans reference this master record instead of redefining collateral metadata.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(name = "product_collateral_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollateralTypeEntity extends AbstractAuditableUuidEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    @Column(name = "handler_code", nullable = false, length = 100)
    private String handlerCode;
    @Column(name = "requires_physical_delivery", nullable = false)
    private boolean requiresPhysicalDelivery;
    @Column(name = "description", length = 1000)
    private String description;
    @Column(name = "active", nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "collateralType", cascade = CascadeType.ALL, orphanRemoval = true)
    private final java.util.Set<CollateralDocumentRequirementEntity> documentRequirements =
            new java.util.LinkedHashSet<>();

    public CollateralTypeEntity(
            String code, String title, String handlerCode, boolean requiresPhysicalDelivery,
            String description, boolean active
    ) {
        this.code = normalize(code, "code");
        update(title, handlerCode, requiresPhysicalDelivery, description, active);
    }

    public void update(
            String title, String handlerCode, boolean requiresPhysicalDelivery, String description, boolean active
    ) {
        this.title = Preconditions.requireText(title, "title").strip();
        this.handlerCode = normalize(handlerCode, "handlerCode");
        this.requiresPhysicalDelivery = requiresPhysicalDelivery;
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.active = active;
    }

    public void replaceDocumentRequirements(
            java.util.Collection<CollateralDocumentRequirementEntity> requirements
    ) {
        documentRequirements.clear();
        documentRequirements.addAll(Preconditions.requireNonNull(requirements, "requirements"));
    }

    private static String normalize(String value, String field) {
        return Preconditions.requireText(value, field).strip().toUpperCase();
    }
}
