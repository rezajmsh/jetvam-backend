package ir.jetvam.modules.identity.persistence;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableAssignedUuidEntity;
import ir.jetvam.modules.identity.model.CustomerOnboardingStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks customer-specific onboarding state on top of the shared party identity.
 * Canonical mobile and Shahkar facts remain attached to the individual party.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Entity
@Table(name = "iam_customer_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerProfileEntity extends AbstractAuditableAssignedUuidEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "id", nullable = false)
    private PartyEntity party;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, length = 40)
    private CustomerOnboardingStatus onboardingStatus;

    @Column(name = "bank_card_number", length = 16)
    private String bankCardNumber;

    @Column(name = "landline", length = 20)
    private String landline;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "address", length = 1000)
    private String address;

    @Column(name = "personal_information_revision", nullable = false)
    private long personalInformationRevision;

    @Column(name = "education_code", length = 80)
    private String educationCode;

    @Column(name = "employment_code", length = 80)
    private String employmentCode;

    @Column(name = "monthly_income", precision = 19, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "employment_information_revision", nullable = false)
    private long employmentInformationRevision;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "iam_customer_employment_document",
            joinColumns = @JoinColumn(name = "customer_profile_id", nullable = false)
    )
    @Column(name = "document_id", nullable = false)
    private final Set<UUID> employmentDocumentIds = new LinkedHashSet<>();

    public CustomerProfileEntity(PartyEntity party) {
        this.party = party;
        this.onboardingStatus = CustomerOnboardingStatus.MOBILE_PENDING;
    }

    public void markMobileVerified() {
        this.onboardingStatus = CustomerOnboardingStatus.MOBILE_VERIFIED;
    }

    public void markIdentityVerified() {
        this.onboardingStatus = CustomerOnboardingStatus.IDENTITY_VERIFIED;
    }

    public void complete() {
        this.onboardingStatus = CustomerOnboardingStatus.COMPLETED;
    }

    public void updatePersonalInformation(
            String bankCardNumber,
            String landline,
            String postalCode,
            String address
    ) {
        this.bankCardNumber = digits(bankCardNumber, 16, "bankCardNumber");
        this.landline = maximumLength(landline, 20, "landline");
        this.postalCode = digits(postalCode, 10, "postalCode");
        this.address = maximumLength(address, 1000, "address");
        this.personalInformationRevision = Math.addExact(personalInformationRevision, 1);
    }

    public void updateEmploymentInformation(
            String educationCode,
            String employmentCode,
            BigDecimal monthlyIncome,
            Collection<UUID> documentIds
    ) {
        this.educationCode = maximumLength(educationCode, 80, "educationCode");
        this.employmentCode = maximumLength(employmentCode, 80, "employmentCode");
        this.monthlyIncome = Preconditions.requireNonNull(monthlyIncome, "monthlyIncome");
        Preconditions.require(monthlyIncome.signum() >= 0, "monthlyIncome must not be negative");
        this.employmentDocumentIds.clear();
        if (documentIds != null) {
            Preconditions.require(documentIds.size() <= 20, "documentIds must contain at most 20 items");
            documentIds.forEach(documentId -> this.employmentDocumentIds.add(
                    Preconditions.requireNonNull(documentId, "documentId")
            ));
        }
        this.employmentInformationRevision = Math.addExact(employmentInformationRevision, 1);
    }

    public boolean hasPersonalInformation() {
        return personalInformationRevision > 0;
    }

    public boolean hasEmploymentInformation() {
        return employmentInformationRevision > 0;
    }

    private static String normalized(String value, String field) {
        return Preconditions.requireText(value, field).strip();
    }

    private static String maximumLength(String value, int maximum, String field) {
        String normalized = normalized(value, field);
        Preconditions.require(normalized.length() <= maximum, field + " is too long");
        return normalized;
    }

    private static String digits(String value, int length, String field) {
        String normalized = normalized(value, field);
        Preconditions.require(
                normalized.length() == length
                        && normalized.chars().allMatch(character -> character >= '0' && character <= '9'),
                field + " must contain exactly " + length + " digits"
        );
        return normalized;
    }
}
