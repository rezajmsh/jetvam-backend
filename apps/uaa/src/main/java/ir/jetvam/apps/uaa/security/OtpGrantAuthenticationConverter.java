package ir.jetvam.apps.uaa.security;

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
 * Converts a strict OTP token request into the custom authorization grant token.
 * Duplicate and malformed parameters are rejected using OAuth error semantics.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public final class OtpGrantAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        if (!OtpGrantConstants.GRANT_TYPE_VALUE.equals(request.getParameter("grant_type"))) {
            return null;
        }
        String challengeValue = requiredSingle(request, OtpGrantConstants.CHALLENGE_ID);
        String otp = requiredSingle(request, OtpGrantConstants.OTP);
        UUID challengeId;
        try {
            challengeId = UUID.fromString(challengeValue);
        } catch (IllegalArgumentException exception) {
            throw invalidRequest(OtpGrantConstants.CHALLENGE_ID + " is invalid");
        }
        String scopeValue = optionalSingle(request, OtpGrantConstants.SCOPE);
        Set<String> scopes = scopeValue == null || scopeValue.isBlank()
                ? Set.of()
                : Arrays.stream(scopeValue.strip().split("\\s+"))
                        .filter(value -> !value.isBlank())
                        .collect(Collectors.toUnmodifiableSet());
        Map<String, Object> additional = new LinkedHashMap<>();
        additional.put(OtpGrantConstants.CHALLENGE_ID, challengeValue);
        return new OtpGrantAuthenticationToken(
                SecurityContextHolder.getContext().getAuthentication(),
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
