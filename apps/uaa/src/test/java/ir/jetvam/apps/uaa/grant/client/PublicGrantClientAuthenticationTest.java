package ir.jetvam.apps.uaa.grant.client;

import ir.jetvam.apps.uaa.grant.password.PasswordGrantConstants;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicGrantClientAuthenticationTest {

    private final PublicGrantClientAuthenticationConverter converter =
            new PublicGrantClientAuthenticationConverter();

    @Test
    void authenticatesRegisteredPublicClientForPasswordGrant() {
        MockHttpServletRequest request = tokenRequest(PasswordGrantConstants.GRANT_TYPE_VALUE);
        request.setParameter("client_id", "jetvam-backoffice");
        OAuth2ClientAuthenticationToken unauthenticated =
                (OAuth2ClientAuthenticationToken) converter.convert(request);

        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("jetvam-backoffice")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(PasswordGrantConstants.GRANT_TYPE)
                .build();
        RegisteredClientRepository repository = mock(RegisteredClientRepository.class);
        when(repository.findByClientId("jetvam-backoffice")).thenReturn(registeredClient);

        OAuth2ClientAuthenticationToken authenticated = (OAuth2ClientAuthenticationToken)
                new PublicGrantClientAuthenticationProvider(repository).authenticate(unauthenticated);

        assertThat(authenticated).isNotNull();
        assertThat(authenticated.isAuthenticated()).isTrue();
        assertThat(authenticated.getRegisteredClient()).isEqualTo(registeredClient);
        assertThat(authenticated.getClientAuthenticationMethod()).isEqualTo(ClientAuthenticationMethod.NONE);
    }

    @Test
    void leavesAuthorizationCodeRequestsForSpringsPkceProvider() {
        MockHttpServletRequest request = tokenRequest(AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
        request.setParameter("client_id", "jetvam-portal");

        assertThat(converter.convert(request)).isNull();
    }

    @Test
    void doesNotTreatRequestsContainingClientCredentialsAsPublicClients() {
        MockHttpServletRequest request = tokenRequest(PasswordGrantConstants.GRANT_TYPE_VALUE);
        request.setParameter("client_id", "jetvam-backoffice");
        request.setParameter("client_secret", "secret");

        assertThat(converter.convert(request)).isNull();
    }

    @Test
    void rejectsMissingClientIdForSupportedPublicGrant() {
        MockHttpServletRequest request = tokenRequest(PasswordGrantConstants.GRANT_TYPE_VALUE);

        assertThatThrownBy(() -> converter.convert(request))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void rejectsClientNotRegisteredForRequestedGrant() {
        MockHttpServletRequest request = tokenRequest(PasswordGrantConstants.GRANT_TYPE_VALUE);
        request.setParameter("client_id", "jetvam-portal");
        OAuth2ClientAuthenticationToken unauthenticated =
                (OAuth2ClientAuthenticationToken) converter.convert(request);
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("jetvam-portal")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .build();
        RegisteredClientRepository repository = mock(RegisteredClientRepository.class);
        when(repository.findByClientId("jetvam-portal")).thenReturn(registeredClient);

        assertThatThrownBy(() -> new PublicGrantClientAuthenticationProvider(repository)
                .authenticate(unauthenticated))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(exception -> assertThat(((OAuth2AuthenticationException) exception)
                        .getError().getErrorCode()).isEqualTo("invalid_client"));
    }

    private static MockHttpServletRequest tokenRequest(String grantType) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");
        request.setParameter("grant_type", grantType);
        return request;
    }
}
