package ir.jetvam.modules.identity.service;

import ir.jetvam.modules.identity.model.IdentityVerificationStatus;
import ir.jetvam.modules.identity.model.MobileVerificationStatus;
import ir.jetvam.modules.identity.model.ShahkarStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Exposes canonical natural-person identity shared by one or more user accounts.
 * Administrators use the party identifier to recognize accounts belonging to the same person.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
public record IndividualPartyView(
        UUID id,
        String displayName,
        String nationalCode,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String mobile,
        MobileVerificationStatus mobileVerificationStatus,
        Instant mobileVerifiedAt,
        ShahkarStatus shahkarStatus,
        Instant shahkarVerifiedAt,
        String shahkarTrackingId,
        IdentityVerificationStatus identityVerificationStatus,
        Instant identityVerifiedAt,
        String bankCardNumber,
        String landline,
        String postalCode,
        String address,
        boolean identityInformationComplete,
        boolean personalInformationComplete
) {
}
