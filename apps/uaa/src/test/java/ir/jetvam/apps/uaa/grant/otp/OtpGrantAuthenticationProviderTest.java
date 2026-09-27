package ir.jetvam.apps.uaa.grant.otp;

import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.modules.identity.exception.CustomerAccountUnavailableException;
import ir.jetvam.modules.identity.exception.CustomerRegistrationRequiredException;
import ir.jetvam.modules.identity.service.CustomerAuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies the public OAuth errors produced by the customer OTP grant.
 * Registration failures remain distinguishable from invalid or expired OTPs.
 *
 * @author reza jamshidi
 * @since 9/27/2026
 */
class OtpGrantAuthenticationProviderTest {

    private CustomerAuthenticationService authenticationService;
    private OtpGrantAuthenticationProvider provider;
    private UUID challengeId;
    private OtpGrantAuthenticationToken grant;

    @BeforeEach
    void setUp() {
        authenticationService = mock(CustomerAuthenticationService.class);
        provider = new OtpGrantAuthenticationProvider(
                authenticationService,
                mock(OAuth2AuthorizationService.class),
                mock(OAuth2TokenGenerator.class)
        );
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("jetvam-portal")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(OtpGrantConstants.GRANT_TYPE)
                .scope("jetvam.api")
                .build();
        OAuth2ClientAuthenticationToken clientPrincipal = new OAuth2ClientAuthenticationToken(
                client,
                ClientAuthenticationMethod.NONE,
                null
        );
        challengeId = UUID.randomUUID();
        grant = new OtpGrantAuthenticationToken(
                clientPrincipal,
                challengeId,
                "123456",
                Set.of("jetvam.api"),
                Map.of()
        );
    }

    @Test
    void exposesRegistrationRequiredAfterOtpWasVerified() {
        when(authenticationService.authenticate(challengeId, "123456"))
                .thenThrow(new CustomerRegistrationRequiredException());

        assertThatThrownBy(() -> provider.authenticate(grant))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, exception -> {
                    assertThat(exception.getError().getErrorCode())
                            .isEqualTo(OtpGrantConstants.CUSTOMER_REGISTRATION_REQUIRED);
                    assertThat(exception.getError().getDescription())
                            .isEqualTo("Customer registration must be completed before signing in");
                });
    }

    @Test
    void exposesUnavailableAccountSeparatelyFromOtpFailure() {
        when(authenticationService.authenticate(challengeId, "123456"))
                .thenThrow(new CustomerAccountUnavailableException());

        assertThatThrownBy(() -> provider.authenticate(grant))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, exception ->
                        assertThat(exception.getError().getErrorCode())
                                .isEqualTo(OtpGrantConstants.CUSTOMER_ACCOUNT_UNAVAILABLE)
                );
    }

    @Test
    void retainsInvalidGrantForAnInvalidOrExpiredOtp() {
        when(authenticationService.authenticate(challengeId, "123456"))
                .thenThrow(new ValidationException("OTP is invalid or expired"));

        assertThatThrownBy(() -> provider.authenticate(grant))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, exception -> {
                    assertThat(exception.getError().getErrorCode()).isEqualTo(OAuth2ErrorCodes.INVALID_GRANT);
                    assertThat(exception.getError().getDescription()).isEqualTo("OTP is invalid or expired");
                });
    }
}
