package ir.jetvam.modules.identity.repository;

import ir.jetvam.modules.identity.model.UserAccountStatus;

import java.time.Instant;

/**
 * Projects only account fields required to accept or reject an access token.
 * The narrow projection avoids loading identity data on every authenticated request.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
public interface UserAccountSecurityState {

    UserAccountStatus getStatus();

    Instant getAuthenticationChangedAt();

    long getAuthenticationVersion();
}
