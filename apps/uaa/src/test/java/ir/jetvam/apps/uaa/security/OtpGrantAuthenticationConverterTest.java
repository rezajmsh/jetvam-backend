package ir.jetvam.apps.uaa.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies strict parsing of OTP parameters at the OAuth token endpoint.
 * Malformed and duplicate security credentials are rejected before authentication.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
class OtpGrantAuthenticationConverterTest {

    private final OtpGrantAuthenticationConverter converter = new OtpGrantAuthenticationConverter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void convertsPurposeSpecificOtpGrant() {
        UUID challengeId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("grant_type", OtpGrantConstants.GRANT_TYPE_VALUE);
        request.addParameter("challenge_id", challengeId.toString());
        request.addParameter("otp", "123456");
        request.addParameter("scope", "jetvam.api offline_access");
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("client", null));

        OtpGrantAuthenticationToken result = (OtpGrantAuthenticationToken) converter.convert(request);

        assertThat(result.challengeId()).isEqualTo(challengeId);
        assertThat(result.otp()).isEqualTo("123456");
        assertThat(result.requestedScopes()).containsExactlyInAnyOrder("jetvam.api", "offline_access");
    }

    @Test
    void rejectsDuplicateOtpParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("grant_type", OtpGrantConstants.GRANT_TYPE_VALUE);
        request.addParameter("challenge_id", UUID.randomUUID().toString());
        request.addParameter("otp", "123456", "654321");

        assertThatThrownBy(() -> converter.convert(request))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
