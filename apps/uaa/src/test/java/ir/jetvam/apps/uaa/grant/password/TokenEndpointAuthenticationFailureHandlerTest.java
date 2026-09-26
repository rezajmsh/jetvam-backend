package ir.jetvam.apps.uaa.grant.password;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import ir.jetvam.infra.web.request.WebRequestAttributes;
import ir.jetvam.modules.otp.service.OtpChallengeView;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the frontend contract returned when a valid password requires an OTP.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
class TokenEndpointAuthenticationFailureHandlerTest {

    @Test
    void writesSafeSecondFactorChallengeMetadata() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        TokenEndpointAuthenticationFailureHandler handler =
                new TokenEndpointAuthenticationFailureHandler(objectMapper);
        UUID challengeId = UUID.randomUUID();
        Instant expiresAt = Instant.parse("2026-09-24T10:02:00Z");
        Instant resendAt = Instant.parse("2026-09-24T10:01:00Z");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                new MockHttpServletRequest(),
                response,
                new PasswordSecondFactorRequiredException(
                        new OtpChallengeView(challengeId, expiresAt, resendAt)
                )
        );

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(body.get("error").asText()).isEqualTo("second_factor_required");
        assertThat(body.get("challenge_id").asText()).isEqualTo(challengeId.toString());
        assertThat(body.get("expires_at").asText()).isEqualTo(expiresAt.toString());
        assertThat(body.get("resend_available_at").asText()).isEqualTo(resendAt.toString());
    }

    @Test
    void exposesHandledOAuthFailureToHttpLogsAndCurrentTrace() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.tag(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(span);
        when(span.event(org.mockito.ArgumentMatchers.anyString())).thenReturn(span);
        when(span.error(org.mockito.ArgumentMatchers.any())).thenReturn(span);
        TokenEndpointAuthenticationFailureHandler handler =
                new TokenEndpointAuthenticationFailureHandler(objectMapper, tracer);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");
        request.setParameter("grant_type", "password");
        request.addHeader("Authorization", "Basic amV0dmFtLWJhY2tvZmZpY2U6aW52YWxpZA==");
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationException failure = new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT, "Client authentication failed", null)
        );

        handler.onAuthenticationFailure(request, response, failure);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(request.getAttribute(WebRequestAttributes.ROUTE)).isEqualTo("/oauth2/token");
        assertThat(request.getAttribute(WebRequestAttributes.OAUTH2_ERROR_CODE)).isEqualTo("invalid_client");
        assertThat(request.getAttribute(WebRequestAttributes.OAUTH2_GRANT_TYPE)).isEqualTo("password");
        assertThat(request.getAttribute(WebRequestAttributes.OAUTH2_CLIENT_AUTHENTICATION_METHOD))
                .isEqualTo("client_secret_basic");
        assertThat(request.getAttribute(WebRequestAttributes.ERROR_MESSAGE))
                .isEqualTo("Client authentication failed");
        verify(span).tag("oauth2.error.code", "invalid_client");
        verify(span).event("oauth2.authentication.failed");
        verify(span).error(failure);
    }

    @Test
    void recordsPublicClientAuthenticationWithoutLoggingCredentials() throws Exception {
        TokenEndpointAuthenticationFailureHandler handler =
                new TokenEndpointAuthenticationFailureHandler(new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");
        request.setParameter("client_id", "jetvam-backoffice");
        request.setParameter("client_secret", "must-not-appear-in-diagnostics");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
                request,
                response,
                new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT))
        );

        assertThat(request.getAttribute(WebRequestAttributes.OAUTH2_CLIENT_ID))
                .isEqualTo("jetvam-backoffice");
        assertThat(request.getAttribute(WebRequestAttributes.OAUTH2_CLIENT_AUTHENTICATION_METHOD))
                .isEqualTo("client_secret_post");
        assertThat(request.getAttribute(WebRequestAttributes.ERROR_MESSAGE).toString())
                .doesNotContain("must-not-appear-in-diagnostics");
    }
}
