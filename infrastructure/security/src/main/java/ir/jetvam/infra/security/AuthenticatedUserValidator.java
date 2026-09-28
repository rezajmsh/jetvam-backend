package ir.jetvam.infra.security;

import ir.jetvam.common.security.AuthenticatedUser;

import java.time.Instant;

/**
 * Validates application-specific account state after JWT signature verification.
 * Resource servers discover validators without depending on a business module.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
@FunctionalInterface
public interface AuthenticatedUserValidator {

    void validate(AuthenticatedUser user, Instant tokenIssuedAt);
}
