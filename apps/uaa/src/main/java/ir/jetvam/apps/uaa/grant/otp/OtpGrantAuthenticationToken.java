package ir.jetvam.apps.uaa.grant.otp;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Carries a validated OTP grant request into Spring Authorization Server.
 * The authenticated OAuth client remains the grant principal.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public final class OtpGrantAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    private final UUID challengeId;
    private final String otp;
    private final Set<String> requestedScopes;

    public OtpGrantAuthenticationToken(
            Authentication clientPrincipal,
            UUID challengeId,
            String otp,
            Set<String> requestedScopes,
            Map<String, Object> additionalParameters
    ) {
        super(OtpGrantConstants.GRANT_TYPE, clientPrincipal, additionalParameters);
        this.challengeId = challengeId;
        this.otp = otp;
        this.requestedScopes = Set.copyOf(requestedScopes);
    }

    public UUID challengeId() {
        return challengeId;
    }

    public String otp() {
        return otp;
    }

    public Set<String> requestedScopes() {
        return requestedScopes;
    }
}
