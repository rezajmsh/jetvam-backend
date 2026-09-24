package ir.jetvam.apps.uaa.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Carries password credentials and an optional required OTP into token authentication.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public final class PasswordGrantAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    private final String username;
    private final String password;
    private final UUID challengeId;
    private final String otp;
    private final Set<String> requestedScopes;

    public PasswordGrantAuthenticationToken(
            Authentication clientPrincipal,
            String username,
            String password,
            UUID challengeId,
            String otp,
            Set<String> requestedScopes,
            Map<String, Object> additionalParameters
    ) {
        super(PasswordGrantConstants.GRANT_TYPE, clientPrincipal, additionalParameters);
        this.username = username;
        this.password = password;
        this.challengeId = challengeId;
        this.otp = otp;
        this.requestedScopes = Set.copyOf(requestedScopes);
    }

    public String username() { return username; }
    public String password() { return password; }
    public UUID challengeId() { return challengeId; }
    public String otp() { return otp; }
    public Set<String> requestedScopes() { return requestedScopes; }
}
