package ir.jetvam.apps.uaa.api;

import ir.jetvam.modules.identity.service.PasswordLoginPreparation;

import java.time.Instant;
import java.util.UUID;

/**
 * Tells the frontend whether the token exchange must include a delivered OTP.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public record PasswordLoginPreparationView(
        boolean secondFactorRequired,
        UUID challengeId,
        Instant expiresAt,
        Instant resendAvailableAt
) {
    public static PasswordLoginPreparationView from(PasswordLoginPreparation preparation) {
        if (preparation.challenge() == null) {
            return new PasswordLoginPreparationView(false, null, null, null);
        }
        return new PasswordLoginPreparationView(
                true,
                preparation.challenge().challengeId(),
                preparation.challenge().expiresAt(),
                preparation.challenge().resendAvailableAt()
        );
    }
}
