package ir.jetvam.apps.uaa.grant.password;

import ir.jetvam.common.exception.RateLimitExceededException;
import ir.jetvam.modules.identity.service.PasswordAuthenticationResult;
import ir.jetvam.modules.identity.service.PasswordUserAuthenticationService;
import ir.jetvam.modules.otp.service.OtpChallengeView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that the password grant pauses token issuance only when an OTP is required.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
class PasswordGrantAuthenticationProviderTest {

    private PasswordUserAuthenticationService authenticationService;
    private PasswordGrantAuthenticationProvider provider;
    private PasswordGrantAuthenticationToken grant;

    @BeforeEach
    void setUp() {
        authenticationService = mock(PasswordUserAuthenticationService.class);
        provider = new PasswordGrantAuthenticationProvider(
                authenticationService,
                mock(OAuth2AuthorizationService.class),
                mock(OAuth2TokenGenerator.class)
        );
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("jetvam-backoffice")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(PasswordGrantConstants.GRANT_TYPE)
                .scope("jetvam.api")
                .build();
        OAuth2ClientAuthenticationToken clientPrincipal = new OAuth2ClientAuthenticationToken(
                client,
                ClientAuthenticationMethod.NONE,
                null
        );
        grant = new PasswordGrantAuthenticationToken(
                clientPrincipal,
                "operator",
                "correct-password",
                null,
                null,
                Set.of("jetvam.api"),
                Map.of()
        );
    }

    @Test
    void exposesChallengeWhenSecondFactorIsRequired() {
        OtpChallengeView challenge = new OtpChallengeView(
                UUID.randomUUID(),
                Instant.parse("2026-09-24T10:02:00Z"),
                Instant.parse("2026-09-24T10:01:00Z")
        );
        when(authenticationService.authenticate("operator", "correct-password", null, null))
                .thenReturn(PasswordAuthenticationResult.secondFactorRequired(challenge));

        assertThatThrownBy(() -> provider.authenticate(grant))
                .isInstanceOfSatisfying(PasswordSecondFactorRequiredException.class, exception -> {
                    assertThat(exception.getError().getErrorCode()).isEqualTo("second_factor_required");
                    assertThat(exception.challenge()).isEqualTo(challenge);
                });
    }

    @Test
    void mapsOtpRateLimitToStandardTemporaryOAuthFailure() {
        when(authenticationService.authenticate("operator", "correct-password", null, null))
                .thenThrow(new RateLimitExceededException(
                        "Too many OTP requests",
                        Instant.parse("2026-09-24T10:05:00Z")
                ));

        assertThatThrownBy(() -> provider.authenticate(grant))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, exception ->
                        assertThat(exception.getError().getErrorCode())
                                .isEqualTo(OAuth2ErrorCodes.TEMPORARILY_UNAVAILABLE)
                );
    }
}
