package ir.jetvam.modules.product.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a commercial product family that groups customer-selectable plans.
 * Plan rules remain outside this entity so the product stays a small catalog boundary.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Entity
@Table(name = "product_catalog_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductEntity extends AbstractAuditableUuidEntity {

    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PublicationStatus status;

    public ProductEntity(String code, String name, String description) {
        this.code = normalizeCode(code);
        revise(name, description);
        this.status = PublicationStatus.DRAFT;
    }

    public void revise(String name, String description) {
        this.name = Preconditions.requireText(name, "name").strip();
        this.description = stripToNull(description);
    }

    public void changeStatus(PublicationStatus status) {
        this.status = Preconditions.requireNonNull(status, "status");
    }

    private static String normalizeCode(String value) {
        return Preconditions.requireText(value, "code").strip().toUpperCase();
    }

    private static String stripToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
