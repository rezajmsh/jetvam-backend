package ir.jetvam.apps.uaa.grant.password;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ErrorAuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds OTP challenge metadata to the password grant's two-factor-required response.
 * All other OAuth failures retain Spring Authorization Server's standard handling.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@RequiredArgsConstructor
public final class TokenEndpointAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;
    private final AuthenticationFailureHandler delegate = new OAuth2ErrorAuthenticationFailureHandler();

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        if (!(exception instanceof PasswordSecondFactorRequiredException secondFactor)) {
            delegate.onAuthenticationFailure(request, response, exception);
            return;
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", PasswordSecondFactorRequiredException.ERROR_CODE);
        body.put("error_description", secondFactor.getError().getDescription());
        body.put("challenge_id", secondFactor.challenge().challengeId());
        body.put("expires_at", secondFactor.challenge().expiresAt());
        body.put("resend_available_at", secondFactor.challenge().resendAvailableAt());

        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
