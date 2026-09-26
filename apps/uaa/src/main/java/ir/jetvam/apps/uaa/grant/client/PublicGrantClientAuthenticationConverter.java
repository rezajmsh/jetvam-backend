package ir.jetvam.apps.uaa.grant.client;

import ir.jetvam.apps.uaa.grant.otp.OtpGrantConstants;
import ir.jetvam.apps.uaa.grant.password.PasswordGrantConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.util.Map;
import java.util.Set;

/**
 * Identifies public clients for the token grants that authenticate the resource owner instead of a client secret.
 * Spring's default public-client converter only handles authorization-code requests with PKCE.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
public final class PublicGrantClientAuthenticationConverter implements AuthenticationConverter {

    private static final Set<String> SUPPORTED_GRANT_TYPES = Set.of(
            OtpGrantConstants.GRANT_TYPE_VALUE,
            PasswordGrantConstants.GRANT_TYPE_VALUE,
            AuthorizationGrantType.REFRESH_TOKEN.getValue()
    );

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = singleValue(request, "grant_type");
        if (!SUPPORTED_GRANT_TYPES.contains(grantType) || hasClientCredentials(request)) {
            return null;
        }

        String clientId = singleValue(request, "client_id");
        if (clientId == null || clientId.isBlank()) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
        }

        return new OAuth2ClientAuthenticationToken(
                clientId,
                ClientAuthenticationMethod.NONE,
                null,
                Map.of("grant_type", grantType)
        );
    }

    private static boolean hasClientCredentials(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        return authorization != null
                || request.getParameter("client_secret") != null
                || request.getParameter("client_assertion") != null;
    }

    private static String singleValue(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        return values != null && values.length == 1 ? values[0] : null;
    }
}
