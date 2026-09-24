package ir.jetvam.modules.identity.service;

import ir.jetvam.modules.identity.security.IdentityUserPrincipal;

import java.util.UUID;

/**
 * Authenticates system and merchant users with password and optional policy-driven OTP.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public interface PasswordUserAuthenticationService {

    PasswordLoginPreparation prepare(String username, String password);

    IdentityUserPrincipal authenticate(
            String username,
            String password,
            UUID challengeId,
            String otp
    );
}
