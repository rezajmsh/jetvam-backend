package ir.jetvam.modules.identity.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Exposes safe OTP challenge metadata without the generated credential.
 * Clients use the identifier for verification before the stated expiry.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record OtpChallengeView(UUID challengeId, Instant expiresAt, Instant resendAvailableAt) {
}
