package ir.jetvam.apps.uaa.grant.password;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.web.authentication.AuthenticationConverter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Strictly parses the password grant and requires challenge and OTP as an atomic pair.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public final class PasswordGrantAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        if (!PasswordGrantConstants.GRANT_TYPE_VALUE.equals(request.getParameter("grant_type"))) {
            return null;
        }
        String username = requiredSingle(request, PasswordGrantConstants.USERNAME);
        String password = requiredSingle(request, PasswordGrantConstants.PASSWORD);
        String challengeValue = optionalSingle(request, PasswordGrantConstants.CHALLENGE_ID);
        String otp = optionalSingle(request, PasswordGrantConstants.OTP);
        if ((challengeValue == null) != (otp == null)) {
            throw invalidRequest("challenge_id and otp must be provided together");
        }
        UUID challengeId = null;
        if (challengeValue != null) {
            try {
                challengeId = UUID.fromString(challengeValue);
            } catch (IllegalArgumentException exception) {
                throw invalidRequest("challenge_id is invalid");
            }
        }
        String scopeValue = optionalSingle(request, PasswordGrantConstants.SCOPE);
        Set<String> scopes = scopeValue == null || scopeValue.isBlank()
                ? Set.of()
                : Arrays.stream(scopeValue.strip().split("\\s+"))
                        .filter(value -> !value.isBlank())
                        .collect(Collectors.toUnmodifiableSet());
        Map<String, Object> additional = new LinkedHashMap<>();
        additional.put(PasswordGrantConstants.USERNAME, username);
        if (challengeValue != null) {
            additional.put(PasswordGrantConstants.CHALLENGE_ID, challengeValue);
        }
        return new PasswordGrantAuthenticationToken(
                SecurityContextHolder.getContext().getAuthentication(),
                username,
                password,
                challengeId,
                otp,
                scopes,
                additional
        );
    }

    private static String requiredSingle(HttpServletRequest request, String name) {
        String value = optionalSingle(request, name);
        if (value == null || value.isBlank()) {
            throw invalidRequest(name + " is required");
        }
        return value;
    }

    private static String optionalSingle(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        if (values == null) {
            return null;
        }
        if (values.length != 1) {
            throw invalidRequest(name + " must be provided once");
        }
        return values[0];
    }

    private static OAuth2AuthenticationException invalidRequest(String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, description, null));
    }
}
