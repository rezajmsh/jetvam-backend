package ir.jetvam.modules.identity.service;

import ir.jetvam.modules.identity.security.IdentityUserPrincipal;
import ir.jetvam.modules.otp.service.OtpChallengeView;

/**
 * Represents either a completed password authentication or a required OTP challenge.
 * The token endpoint uses this result to finish one-factor login immediately and pause only two-factor login.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record PasswordAuthenticationResult(
        IdentityUserPrincipal principal,
        OtpChallengeView challenge
) {

    public PasswordAuthenticationResult {
        if ((principal == null) == (challenge == null)) {
            throw new IllegalArgumentException("Exactly one authentication result must be present");
        }
    }

    public static PasswordAuthenticationResult authenticated(IdentityUserPrincipal principal) {
        return new PasswordAuthenticationResult(principal, null);
    }

    public static PasswordAuthenticationResult secondFactorRequired(OtpChallengeView challenge) {
        return new PasswordAuthenticationResult(null, challenge);
    }

    public boolean requiresSecondFactor() {
        return challenge != null;
    }
}
