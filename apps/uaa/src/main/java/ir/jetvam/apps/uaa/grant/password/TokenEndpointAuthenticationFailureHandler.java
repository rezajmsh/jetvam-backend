package ir.jetvam.apps.uaa.grant.password;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import ir.jetvam.infra.web.request.WebRequestAttributes;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ErrorAuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Adds OTP challenge metadata to the password grant's two-factor-required response.
 * All other OAuth failures retain Spring Authorization Server's standard handling.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public final class TokenEndpointAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private static final Pattern SAFE_GRANT_TYPE = Pattern.compile("[A-Za-z0-9:._/-]{1,150}");
    private static final Pattern SAFE_CLIENT_ID = Pattern.compile("[A-Za-z0-9._-]{1,100}");
    private static final int MAX_DESCRIPTION_LENGTH = 300;

    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final AuthenticationFailureHandler delegate = new OAuth2ErrorAuthenticationFailureHandler();

    public TokenEndpointAuthenticationFailureHandler(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    public TokenEndpointAuthenticationFailureHandler(ObjectMapper objectMapper, Tracer tracer) {
        this.objectMapper = objectMapper;
        this.tracer = tracer;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        captureDiagnostics(request, exception);
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

    private void captureDiagnostics(HttpServletRequest request, AuthenticationException exception) {
        String errorCode = "authentication_failed";
        String description = "OAuth authentication failed";
        if (exception instanceof OAuth2AuthenticationException oauthException) {
            OAuth2Error error = oauthException.getError();
            errorCode = error.getErrorCode();
            description = safeDescription(error.getDescription(), description);
        }

        request.setAttribute(WebRequestAttributes.ROUTE, safeRoute(request));
        request.setAttribute(WebRequestAttributes.OAUTH2_ERROR_CODE, errorCode);
        request.setAttribute(WebRequestAttributes.ERROR_TYPE, exception.getClass().getName());
        request.setAttribute(WebRequestAttributes.ERROR_MESSAGE, description);

        String grantType = request.getParameter("grant_type");
        if (grantType != null && SAFE_GRANT_TYPE.matcher(grantType).matches()) {
            request.setAttribute(WebRequestAttributes.OAUTH2_GRANT_TYPE, grantType);
        }
        String clientId = request.getParameter("client_id");
        if (clientId != null && SAFE_CLIENT_ID.matcher(clientId).matches()) {
            request.setAttribute(WebRequestAttributes.OAUTH2_CLIENT_ID, clientId);
        }
        String clientAuthenticationMethod = clientAuthenticationMethod(request);
        request.setAttribute(
                WebRequestAttributes.OAUTH2_CLIENT_AUTHENTICATION_METHOD,
                clientAuthenticationMethod
        );

        Span span = tracer == null ? null : tracer.currentSpan();
        if (span != null) {
            span.tag("oauth2.error.code", errorCode)
                    .tag("error.type", exception.getClass().getName())
                    .tag("error.message", description)
                    .event("oauth2.authentication.failed")
                    .error(exception);
            if (grantType != null && SAFE_GRANT_TYPE.matcher(grantType).matches()) {
                span.tag("oauth2.grant.type", grantType);
            }
            if (clientId != null && SAFE_CLIENT_ID.matcher(clientId).matches()) {
                span.tag("oauth2.client.id", clientId);
            }
            span.tag("oauth2.client.authentication_method", clientAuthenticationMethod);
        }
    }

    private static String clientAuthenticationMethod(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            return "client_secret_basic";
        }
        if (request.getParameter("client_assertion") != null) {
            return "client_assertion";
        }
        if (request.getParameter("client_secret") != null) {
            return "client_secret_post";
        }
        if (request.getParameter("client_id") != null) {
            return "none";
        }
        return "missing";
    }

    private static String safeRoute(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path != null && path.startsWith("/") && path.length() <= 200 ? path : "/oauth2/token";
    }

    private static String safeDescription(String candidate, String fallback) {
        if (candidate == null || candidate.isBlank()) {
            return fallback;
        }
        String normalized = candidate.replace('\r', ' ').replace('\n', ' ').strip();
        return normalized.length() <= MAX_DESCRIPTION_LENGTH
                ? normalized
                : normalized.substring(0, MAX_DESCRIPTION_LENGTH);
    }
}
