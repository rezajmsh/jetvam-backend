package ir.jetvam.modules.identity.persistence;

import ir.jetvam.infra.persistence.entity.AbstractAuditableAssignedUuidEntity;
import ir.jetvam.modules.identity.model.CustomerOnboardingStatus;
import jakarta.persistence.Column;
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
}
