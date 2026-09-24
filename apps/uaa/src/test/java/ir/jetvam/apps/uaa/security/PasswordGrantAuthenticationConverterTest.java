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

/** Documents the frontend contract for password and optional second-factor token requests. */
class PasswordGrantAuthenticationConverterTest {

    private final PasswordGrantAuthenticationConverter converter = new PasswordGrantAuthenticationConverter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void convertsPasswordGrantWithSecondFactor() {
        UUID challengeId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("grant_type", PasswordGrantConstants.GRANT_TYPE_VALUE);
        request.addParameter("username", "operator");
        request.addParameter("password", "secret-password");
        request.addParameter("challenge_id", challengeId.toString());
        request.addParameter("otp", "123456");
        request.addParameter("scope", "jetvam.api offline_access");
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("client", null));

        PasswordGrantAuthenticationToken result = (PasswordGrantAuthenticationToken) converter.convert(request);

        assertThat(result.username()).isEqualTo("operator");
        assertThat(result.password()).isEqualTo("secret-password");
        assertThat(result.challengeId()).isEqualTo(challengeId);
        assertThat(result.otp()).isEqualTo("123456");
        assertThat(result.requestedScopes()).containsExactlyInAnyOrder("jetvam.api", "offline_access");
    }

    @Test
    void rejectsOtpWithoutChallenge() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("grant_type", PasswordGrantConstants.GRANT_TYPE_VALUE);
        request.addParameter("username", "operator");
        request.addParameter("password", "secret-password");
        request.addParameter("otp", "123456");

        assertThatThrownBy(() -> converter.convert(request))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
