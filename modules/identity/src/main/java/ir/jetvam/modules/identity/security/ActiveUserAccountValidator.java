package ir.jetvam.modules.identity.security;

import ir.jetvam.common.security.AuthenticatedUser;
import ir.jetvam.infra.security.AuthenticatedUserValidator;
import ir.jetvam.infra.security.SecurityClaims;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import ir.jetvam.modules.identity.repository.UserAccountSecurityState;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Rejects JWTs for missing, inactive or security-modified user accounts.
 * Service principals without a user identifier remain outside this user check.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
@Component
@RequiredArgsConstructor
public class ActiveUserAccountValidator implements AuthenticatedUserValidator {

    private static final OAuth2Error INVALID_TOKEN = new OAuth2Error("invalid_token");

    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public void validate(AuthenticatedUser user, Instant tokenIssuedAt) {
        if (user.userId() == null) {
            return;
        }
        UserAccountSecurityState state = userAccountRepository.findSecurityStateById(user.userId())
                .orElseThrow(ActiveUserAccountValidator::invalidToken);
        if (state.getStatus() != UserAccountStatus.ACTIVE) {
            throw invalidToken();
        }
        Object claimedVersion = user.attributes().get(SecurityClaims.AUTHENTICATION_VERSION);
        if (!(claimedVersion instanceof Number number)
                || number.longValue() != state.getAuthenticationVersion()) {
            throw invalidToken();
        }
    }

    private static OAuth2AuthenticationException invalidToken() {
        return new OAuth2AuthenticationException(INVALID_TOKEN, "Access token is no longer valid");
    }
}
