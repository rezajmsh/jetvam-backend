package ir.jetvam.modules.identity.persistence;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.model.IdentityVerificationStatus;
import ir.jetvam.modules.identity.model.MobileVerificationStatus;
import ir.jetvam.modules.identity.model.PartyType;
import ir.jetvam.modules.identity.model.ShahkarStatus;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Stores personal identity attributes once for every natural-person party.
 * Verification metadata is kept alongside the authoritative identity values.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Entity
@Table(name = "iam_individual_party")
@DiscriminatorValue("INDIVIDUAL")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IndividualPartyEntity extends PartyEntity {

    @Column(name = "national_code", nullable = false, unique = true, length = 10)
    private String nationalCode;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "mobile", unique = true, length = 11)
    private String mobile;

    @Column(name = "mobile_verified_at")
    private Instant mobileVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "mobile_verification_status", nullable = false, length = 30)
    private MobileVerificationStatus mobileVerificationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "shahkar_status", nullable = false, length = 30)
    private ShahkarStatus shahkarStatus;

    @Column(name = "shahkar_verified_at")
    private Instant shahkarVerifiedAt;

    @Column(name = "shahkar_tracking_id", length = 100)
    private String shahkarTrackingId;

    @Column(name = "identity_verified_at")
    private Instant identityVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_verification_status", nullable = false, length = 30)
    private IdentityVerificationStatus identityVerificationStatus;

    @Column(name = "bank_card_number", length = 16)
    private String bankCardNumber;

    @Column(name = "landline", length = 20)
    private String landline;

    @Column(name = "postal_code", length = 10)
    private String postalCode;

    @Column(name = "address", length = 1000)
    private String address;


    public IndividualPartyEntity(
            String displayName,
            String nationalCode,
            String firstName,
            String lastName,
            LocalDate birthDate,
            String mobile
    ) {
        super(displayName);
        this.nationalCode = nationalCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.mobile = mobile;
        this.mobileVerificationStatus = MobileVerificationStatus.UNVERIFIED;
        this.shahkarStatus = ShahkarStatus.NOT_REQUESTED;
        this.identityVerificationStatus = IdentityVerificationStatus.PENDING;
    }

    @Override
    @Transient
    public PartyType getType() {
        return PartyType.INDIVIDUAL;
    }

    public void markMobileVerified(Instant verifiedAt) {
        this.mobileVerifiedAt = verifiedAt;
        this.mobileVerificationStatus = MobileVerificationStatus.VERIFIED;
    }

    public void markShahkarPending() {
        this.shahkarStatus = ShahkarStatus.PENDING;
    }

    public void markShahkarMatched(Instant verifiedAt, String trackingId) {
        this.shahkarStatus = ShahkarStatus.MATCHED;
        this.shahkarVerifiedAt = verifiedAt;
        this.shahkarTrackingId = trackingId;
    }

    public void markShahkarNotMatched(String trackingId) {
        this.shahkarStatus = ShahkarStatus.NOT_MATCHED;
        this.shahkarTrackingId = trackingId;
        this.identityVerificationStatus = IdentityVerificationStatus.REJECTED;
    }

    public void markShahkarFailed() {
        this.shahkarStatus = ShahkarStatus.FAILED;
    }

    public void markIdentityVerified(Instant verifiedAt) {
        this.identityVerifiedAt = verifiedAt;
        this.identityVerificationStatus = IdentityVerificationStatus.VERIFIED;
    }

    public void completeIdentity(String firstName, String lastName, LocalDate birthDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        changeDisplayName(firstName + " " + lastName);
    }

    public void updateIdentity(
            String nationalCode,
            String firstName,
            String lastName,
            LocalDate birthDate,
            String mobile
    ) {
        this.nationalCode = nationalCode;
        this.mobile = mobile;
        completeIdentity(firstName, lastName, birthDate);
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
    }

    public boolean hasCompleteIdentityInformation() {
        return firstName != null && !firstName.isBlank()
                && lastName != null && !lastName.isBlank()
                && birthDate != null;
    }

    public boolean hasCompletePersonalInformation() {
        return bankCardNumber != null && landline != null && postalCode != null && address != null;
    }

    public boolean isReadyForApplication() {
        return mobileVerificationStatus == MobileVerificationStatus.VERIFIED
                && shahkarStatus == ShahkarStatus.MATCHED
                && identityVerificationStatus == IdentityVerificationStatus.VERIFIED
                && hasCompleteIdentityInformation()
                && hasCompletePersonalInformation();
    }

    private static String maximumLength(String value, int maximum, String field) {
        String normalized = Preconditions.requireText(value, field).strip();
        Preconditions.require(normalized.length() <= maximum, field + " is too long");
        return normalized;
    }

    private static String digits(String value, int length, String field) {
        String normalized = Preconditions.requireText(value, field).strip();
        Preconditions.require(normalized.length() == length
                        && normalized.chars().allMatch(character -> character >= '0' && character <= '9'),
                field + " must contain exactly " + length + " digits");
        return normalized;
    }

}
