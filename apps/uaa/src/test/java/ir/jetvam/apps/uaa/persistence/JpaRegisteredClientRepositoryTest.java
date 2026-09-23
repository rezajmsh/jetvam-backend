package ir.jetvam.apps.uaa.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies lossless conversion between normalized client records and OAuth clients.
 * Protocol settings must remain intact when clients are loaded for authorization.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
class JpaRegisteredClientRepositoryTest {

    @Test
    void reconstructsEnabledRegisteredClient() {
        OAuthClientJpaRepository repository = mock(OAuthClientJpaRepository.class);
        RegisteredClient source = RegisteredClient.withId("client-record-id")
                .clientId("jetvam-portal")
                .clientIdIssuedAt(Instant.parse("2026-09-22T00:00:00Z"))
                .clientName("Jetvam Portal")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("https://portal.example.test/oauth/callback")
                .postLogoutRedirectUri("https://portal.example.test/")
                .scope("openid")
                .scope("jetvam.api")
                .clientSettings(ClientSettings.builder().requireProofKey(true).build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(15))
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .reuseRefreshTokens(false)
                        .build())
                .build();
        when(repository.findByClientIdAndEnabledTrue("jetvam-portal"))
                .thenReturn(Optional.of(new OAuthClientEntity(source)));

        RegisteredClient result = new JpaRegisteredClientRepository(repository)
                .findByClientId("jetvam-portal");

        assertThat(result).isNotNull();
        assertThat(result.getClientId()).isEqualTo(source.getClientId());
        assertThat(result.getClientAuthenticationMethods()).containsExactly(ClientAuthenticationMethod.NONE);
        assertThat(result.getAuthorizationGrantTypes())
                .containsExactlyInAnyOrder(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN);
        assertThat(result.getRedirectUris()).containsExactly("https://portal.example.test/oauth/callback");
        assertThat(result.getPostLogoutRedirectUris()).containsExactly("https://portal.example.test/");
        assertThat(result.getScopes()).containsExactlyInAnyOrder("openid", "jetvam.api");
        assertThat(result.getClientSettings().isRequireProofKey()).isTrue();
        assertThat(result.getTokenSettings().getAccessTokenTimeToLive()).isEqualTo(Duration.ofMinutes(15));
    }
}
