package ir.jetvam.modules.identity.service;

import ir.jetvam.modules.identity.model.IdentityVerificationStatus;
import ir.jetvam.modules.identity.model.MobileVerificationStatus;
import ir.jetvam.modules.identity.model.ShahkarStatus;

import java.util.UUID;

/**
 * Returns stable account identifiers and the current customer onboarding state.
 * No credential or sensitive national identifier is exposed to the caller.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record CustomerRegistrationResult(
        UUID userId,
        UUID partyId,
        MobileVerificationStatus mobileVerificationStatus,
        ShahkarStatus shahkarStatus,
        IdentityVerificationStatus identityVerificationStatus
) {
}
