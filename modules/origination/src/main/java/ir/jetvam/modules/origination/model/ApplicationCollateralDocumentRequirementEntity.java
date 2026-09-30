package ir.jetvam.modules.origination.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractUuidEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Snapshots one document requirement for an application collateral.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(name = "origination_application_collateral_document_requirement", uniqueConstraints = @UniqueConstraint(
        name = "uk_origination_collateral_document_requirement",
        columnNames = {"application_collateral_id", "document_type_id"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationCollateralDocumentRequirementEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_collateral_id", nullable = false)
    private ApplicationCollateralEntity collateral;
    @Column(name = "document_type_id", nullable = false)
    private UUID documentTypeId;
    @Column(name = "document_type_code", nullable = false, length = 100)
    private String documentTypeCode;
    @Column(name = "document_type_title", nullable = false, length = 200)
    private String documentTypeTitle;
    @Column(name = "allowed_content_types", nullable = false, length = 1000)
    private String allowedContentTypes;
    @Column(name = "maximum_size_bytes", nullable = false)
    private long maximumSizeBytes;
    @Column(name = "required", nullable = false)
    private boolean required;
    @Column(name = "minimum_count", nullable = false)
    private int minimumCount;
    @Column(name = "maximum_count", nullable = false)
    private int maximumCount;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @OneToMany(mappedBy = "collateralDocumentRequirement", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<ApplicationDocumentEntity> documents = new LinkedHashSet<>();

    public ApplicationCollateralDocumentRequirementEntity(
            ApplicationCollateralEntity collateral, UUID documentTypeId, String documentTypeCode,
            String documentTypeTitle, String allowedContentTypes, long maximumSizeBytes,
            boolean required, int minimumCount, int maximumCount, int displayOrder
    ) {
        this.collateral = Preconditions.requireNonNull(collateral, "collateral");
        this.documentTypeId = Preconditions.requireNonNull(documentTypeId, "documentTypeId");
        this.documentTypeCode = normalize(documentTypeCode);
        this.documentTypeTitle = Preconditions.requireText(documentTypeTitle, "documentTypeTitle").strip();
        this.allowedContentTypes = Preconditions.requireText(allowedContentTypes, "allowedContentTypes").strip();
        this.maximumSizeBytes = Preconditions.requirePositive(maximumSizeBytes, "maximumSizeBytes");
        Preconditions.require(minimumCount >= 0, "minimumCount must not be negative");
        Preconditions.require(maximumCount > 0 && maximumCount >= minimumCount,
                "maximumCount must be positive and not less than minimumCount");
        this.required = required;
        this.minimumCount = minimumCount;
        this.maximumCount = maximumCount;
        this.displayOrder = Preconditions.requirePositive(displayOrder, "displayOrder");
    }

    public void addDocument(ApplicationDocumentEntity document) {
        Preconditions.require(activeDocumentCount() < maximumCount, "Maximum document count has been reached");
        documents.add(Preconditions.requireNonNull(document, "document"));
    }

    public boolean isSatisfied() {
        return !required || activeDocumentCount() >= minimumCount;
    }

    public long activeDocumentCount() {
        return documents.stream().filter(item -> item.getStatus() == ApplicationDocumentStatus.ACTIVE).count();
    }

    public boolean accepts(String contentType) {
        String normalized = Preconditions.requireText(contentType, "contentType").strip().toLowerCase();
        return java.util.Arrays.stream(allowedContentTypes.split(","))
                .map(String::strip).map(String::toLowerCase).anyMatch(normalized::equals);
    }

    private static String normalize(String value) {
        return Preconditions.requireText(value, "documentTypeCode").strip().toUpperCase();
    }
}
