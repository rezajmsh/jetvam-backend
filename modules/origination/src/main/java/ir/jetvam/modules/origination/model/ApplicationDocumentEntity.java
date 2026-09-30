package ir.jetvam.modules.origination.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
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

import java.util.UUID;

/**
 * Stores immutable metadata for one application file kept by the document-storage port.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(name = "origination_application_document")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationDocumentEntity extends AbstractAuditableUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplicationEntity application;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collateral_document_requirement_id", nullable = false)
    private ApplicationCollateralDocumentRequirementEntity collateralDocumentRequirement;
    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;
    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;
    @Column(name = "uploaded_by_party_id", nullable = false)
    private UUID uploadedByPartyId;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ApplicationDocumentStatus status;

    public ApplicationDocumentEntity(
            LoanApplicationEntity application,
            ApplicationCollateralDocumentRequirementEntity requirement,
            String storageKey,
            String originalFilename,
            String contentType,
            long sizeBytes,
            String checksumSha256,
            UUID uploadedByPartyId
    ) {
        this.application = Preconditions.requireNonNull(application, "application");
        this.collateralDocumentRequirement = Preconditions.requireNonNull(requirement, "requirement");
        this.storageKey = Preconditions.requireText(storageKey, "storageKey").strip();
        this.originalFilename = Preconditions.requireText(originalFilename, "originalFilename").strip();
        this.contentType = Preconditions.requireText(contentType, "contentType").strip().toLowerCase();
        this.sizeBytes = Preconditions.requirePositive(sizeBytes, "sizeBytes");
        this.checksumSha256 = Preconditions.requireText(checksumSha256, "checksumSha256").strip().toLowerCase();
        this.uploadedByPartyId = Preconditions.requireNonNull(uploadedByPartyId, "uploadedByPartyId");
        this.status = ApplicationDocumentStatus.ACTIVE;
    }
}
