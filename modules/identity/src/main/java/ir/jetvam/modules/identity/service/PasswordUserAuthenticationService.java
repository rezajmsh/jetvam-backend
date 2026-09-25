package ir.jetvam.modules.identity.service;

import java.util.UUID;

/**
 * Authenticates system and merchant users with password and optional policy-driven OTP.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public interface PasswordUserAuthenticationService {

    PasswordAuthenticationResult authenticate(
            String username,
            String password,
            UUID challengeId,
            String otp
    );
}
