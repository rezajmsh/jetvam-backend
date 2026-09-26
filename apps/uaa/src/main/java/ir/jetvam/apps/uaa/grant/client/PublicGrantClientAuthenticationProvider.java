package ir.jetvam.apps.uaa.grant.client;

import ir.jetvam.apps.uaa.grant.otp.OtpGrantConstants;
import ir.jetvam.apps.uaa.grant.password.PasswordGrantConstants;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Set;

/**
 * Authenticates registered public clients for Jetvam's custom grants and refresh-token requests.
 * It deliberately leaves authorization-code requests to Spring's PKCE-aware provider.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
public final class PublicGrantClientAuthenticationProvider implements AuthenticationProvider {

    private static final Set<String> SUPPORTED_GRANT_TYPES = Set.of(
            OtpGrantConstants.GRANT_TYPE_VALUE,
            PasswordGrantConstants.GRANT_TYPE_VALUE,
            AuthorizationGrantType.REFRESH_TOKEN.getValue()
    );
    private static final String ERROR_URI = "https://datatracker.ietf.org/doc/html/rfc6749#section-3.2.1";

    private final RegisteredClientRepository registeredClientRepository;

    public PublicGrantClientAuthenticationProvider(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        OAuth2ClientAuthenticationToken clientAuthentication =
                (OAuth2ClientAuthenticationToken) authentication;
        if (!ClientAuthenticationMethod.NONE.equals(clientAuthentication.getClientAuthenticationMethod())) {
            return null;
        }

        Object grantTypeValue = clientAuthentication.getAdditionalParameters().get("grant_type");
        if (!(grantTypeValue instanceof String grantType) || !SUPPORTED_GRANT_TYPES.contains(grantType)) {
            return null;
        }

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(
                clientAuthentication.getPrincipal().toString()
        );
        if (registeredClient == null
                || !registeredClient.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)
                || !registeredClient.getAuthorizationGrantTypes().contains(new AuthorizationGrantType(grantType))) {
            throw invalidClient();
        }

        return new OAuth2ClientAuthenticationToken(
                registeredClient,
                ClientAuthenticationMethod.NONE,
                null
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static OAuth2AuthenticationException invalidClient() {
        return new OAuth2AuthenticationException(new OAuth2Error(
                OAuth2ErrorCodes.INVALID_CLIENT,
                "Client authentication failed",
                ERROR_URI
        ));
    }
}
