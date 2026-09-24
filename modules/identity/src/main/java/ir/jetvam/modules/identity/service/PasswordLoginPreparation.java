package ir.jetvam.modules.identity.service;

/**
 * Result of validating password credentials and evaluating the configured 2FA policy.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public record PasswordLoginPreparation(
        boolean secondFactorRequired,
        OtpChallengeView challenge
) {
}
