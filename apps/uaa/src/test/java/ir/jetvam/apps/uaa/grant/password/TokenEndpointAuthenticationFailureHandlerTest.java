package ir.jetvam.apps.uaa.grant.password;

import ir.jetvam.modules.otp.service.OtpChallengeView;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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
}
