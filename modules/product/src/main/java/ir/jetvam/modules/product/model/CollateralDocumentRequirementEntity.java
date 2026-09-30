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
 * Associates a document type and cardinality rule with one reusable collateral type.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(
        name = "product_collateral_document_requirement",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_collateral_document", columnNames = {"collateral_type_id", "document_type_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollateralDocumentRequirementEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collateral_type_id", nullable = false)
    private CollateralTypeEntity collateralType;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_type_id", nullable = false)
    private DocumentTypeEntity documentType;
    @Column(name = "required", nullable = false)
    private boolean required;
    @Column(name = "minimum_count", nullable = false)
    private int minimumCount;
    @Column(name = "maximum_count", nullable = false)
    private int maximumCount;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public CollateralDocumentRequirementEntity(
            CollateralTypeEntity collateralType, DocumentTypeEntity documentType, boolean required,
            int minimumCount, int maximumCount, int displayOrder
    ) {
        this.collateralType = Preconditions.requireNonNull(collateralType, "collateralType");
        this.documentType = Preconditions.requireNonNull(documentType, "documentType");
        Preconditions.require(minimumCount >= 0, "minimumCount must not be negative");
        Preconditions.require(maximumCount >= minimumCount && maximumCount > 0,
                "maximumCount must be positive and not less than minimumCount");
        this.required = required;
        this.minimumCount = minimumCount;
        this.maximumCount = maximumCount;
        this.displayOrder = Preconditions.requirePositive(displayOrder, "displayOrder");
    }
}
