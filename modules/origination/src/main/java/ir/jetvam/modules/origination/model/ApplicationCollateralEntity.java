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
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Snapshots one plan collateral requirement and the party role responsible for fulfilling it.
 * Handler-specific details are stored in typed child tables instead of the application row.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(name = "origination_application_collateral")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationCollateralEntity extends AbstractAuditableUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplicationEntity application;
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 20)
    private CollateralProviderType providerType;
    @Column(name = "provider_party_id")
    private UUID providerPartyId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guarantor_id")
    private ApplicationGuarantorEntity guarantor;
    @Column(name = "collateral_type_id", nullable = false)
    private UUID collateralTypeId;
    @Column(name = "collateral_type_code", nullable = false, length = 100)
    private String collateralTypeCode;
    @Column(name = "collateral_type_title", nullable = false, length = 200)
    private String collateralTypeTitle;
    @Column(name = "handler_code", nullable = false, length = 100)
    private String handlerCode;
    @Column(name = "minimum_coverage_percent", nullable = false, precision = 7, scale = 2)
    private BigDecimal minimumCoveragePercent;
    @Column(name = "required", nullable = false)
    private boolean required;
    @Column(name = "requires_physical_delivery", nullable = false)
    private boolean requiresPhysicalDelivery;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ApplicationCollateralStatus status;
    @Column(name = "information_completed_at")
    private Instant informationCompletedAt;
    @Column(name = "original_received_at")
    private Instant originalReceivedAt;

    @OneToMany(mappedBy = "collateral", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<ApplicationCollateralDocumentRequirementEntity> documentRequirements = new LinkedHashSet<>();

    public ApplicationCollateralEntity(
            LoanApplicationEntity application, CollateralProviderType providerType, UUID providerPartyId,
            ApplicationGuarantorEntity guarantor, UUID collateralTypeId, String collateralTypeCode,
            String collateralTypeTitle, String handlerCode, BigDecimal minimumCoveragePercent,
            boolean required, boolean requiresPhysicalDelivery
    ) {
        this.application = Preconditions.requireNonNull(application, "application");
        this.providerType = Preconditions.requireNonNull(providerType, "providerType");
        this.providerPartyId = providerPartyId;
        this.guarantor = guarantor;
        this.collateralTypeId = Preconditions.requireNonNull(collateralTypeId, "collateralTypeId");
        this.collateralTypeCode = normalize(collateralTypeCode, "collateralTypeCode");
        this.collateralTypeTitle = Preconditions.requireText(collateralTypeTitle, "collateralTypeTitle").strip();
        this.handlerCode = normalize(handlerCode, "handlerCode");
        this.minimumCoveragePercent = Preconditions.requireNonNull(minimumCoveragePercent, "minimumCoveragePercent");
        Preconditions.require(minimumCoveragePercent.signum() >= 0, "minimumCoveragePercent must not be negative");
        this.required = required;
        this.requiresPhysicalDelivery = requiresPhysicalDelivery;
        this.status = providerPartyId == null
                ? ApplicationCollateralStatus.WAITING_PROVIDER : ApplicationCollateralStatus.WAITING_INFORMATION;
    }

    public void assignProvider(UUID partyId) {
        this.providerPartyId = Preconditions.requireNonNull(partyId, "partyId");
        if (status == ApplicationCollateralStatus.WAITING_PROVIDER) {
            status = ApplicationCollateralStatus.WAITING_INFORMATION;
        }
    }

    public void informationCompleted(Instant at) {
        Preconditions.require(status == ApplicationCollateralStatus.WAITING_INFORMATION,
                "Collateral is not waiting for information");
        this.informationCompletedAt = Preconditions.requireNonNull(at, "at");
        advanceWhenComplete();
    }

    public void addDocumentRequirement(ApplicationCollateralDocumentRequirementEntity requirement) {
        documentRequirements.add(Preconditions.requireNonNull(requirement, "requirement"));
    }

    public void documentsUpdated() {
        advanceWhenComplete();
    }

    public void originalReceived(Instant at) {
        Preconditions.require(status == ApplicationCollateralStatus.WAITING_ORIGINAL_DELIVERY,
                "Collateral is not waiting for original delivery");
        this.originalReceivedAt = Preconditions.requireNonNull(at, "at");
        this.status = ApplicationCollateralStatus.ACCEPTED;
    }

    private static String normalize(String value, String field) {
        return Preconditions.requireText(value, field).strip().toUpperCase();
    }

    private void advanceWhenComplete() {
        if (status == ApplicationCollateralStatus.WAITING_INFORMATION && informationCompletedAt != null
                && documentRequirements.stream().allMatch(ApplicationCollateralDocumentRequirementEntity::isSatisfied)) {
            this.status = requiresPhysicalDelivery
                    ? ApplicationCollateralStatus.WAITING_ORIGINAL_DELIVERY : ApplicationCollateralStatus.ACCEPTED;
        }
    }
}
