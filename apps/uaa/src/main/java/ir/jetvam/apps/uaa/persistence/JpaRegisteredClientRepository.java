package ir.jetvam.apps.uaa.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Adapts normalized JPA client records to Spring Authorization Server.
 * Client reads and writes therefore receive standard repository observability.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Repository
@RequiredArgsConstructor
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private final OAuthClientJpaRepository repository;

    @Override
    @Transactional
    public void save(RegisteredClient registeredClient) {
        OAuthClientEntity entity = repository.findById(registeredClient.getId())
                .orElseGet(() -> new OAuthClientEntity(registeredClient));
        entity.update(registeredClient);
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findById(String id) {
        return repository.findByIdAndEnabledTrue(id).map(JpaRegisteredClientRepository::toRegisteredClient).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public RegisteredClient findByClientId(String clientId) {
        return repository.findByClientIdAndEnabledTrue(clientId)
                .map(JpaRegisteredClientRepository::toRegisteredClient)
                .orElse(null);
    }

    private static RegisteredClient toRegisteredClient(OAuthClientEntity entity) {
        RegisteredClient.Builder builder = RegisteredClient.withId(entity.getId())
                .clientId(entity.getClientId())
                .clientIdIssuedAt(entity.getClientIdIssuedAt())
                .clientName(entity.getClientName())
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(entity.isRequireProofKey())
                        .requireAuthorizationConsent(entity.isRequireConsent())
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofSeconds(entity.getAccessTokenTtlSeconds()))
                        .refreshTokenTimeToLive(Duration.ofSeconds(entity.getRefreshTokenTtlSeconds()))
                        .reuseRefreshTokens(entity.isReuseRefreshTokens())
                        .build());
        if (entity.getClientSecret() != null) {
            builder.clientSecret(entity.getClientSecret());
        }
        if (entity.getClientSecretExpiresAt() != null) {
            builder.clientSecretExpiresAt(entity.getClientSecretExpiresAt());
        }
        entity.getAuthenticationMethods().stream()
                .map(ClientAuthenticationMethod::new)
                .forEach(builder::clientAuthenticationMethod);
        entity.getGrantTypes().stream()
                .map(AuthorizationGrantType::new)
                .forEach(builder::authorizationGrantType);
        entity.getRedirectUris().forEach(builder::redirectUri);
        entity.getPostLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
        entity.getScopes().forEach(builder::scope);
        return builder.build();
    }
}
