package ir.jetvam.apps.uaa.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Stores an OAuth client and its protocol settings in normalized database tables.
 * It replaces deployment-list configuration as the authoritative client source.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Entity
@Table(name = "uaa_oauth_client")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthClientEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 100)
    private String id;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "client_id", nullable = false, unique = true, length = 100)
    private String clientId;

    @Column(name = "client_id_issued_at", nullable = false)
    private Instant clientIdIssuedAt;

    @Column(name = "client_secret", length = 200)
    private String clientSecret;

    @Column(name = "client_secret_expires_at")
    private Instant clientSecretExpiresAt;

    @Column(name = "client_name", nullable = false, length = 200)
    private String clientName;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "require_proof_key", nullable = false)
    private boolean requireProofKey;

    @Column(name = "require_consent", nullable = false)
    private boolean requireConsent;

    @Column(name = "access_token_ttl_seconds", nullable = false)
    private long accessTokenTtlSeconds;

    @Column(name = "refresh_token_ttl_seconds", nullable = false)
    private long refreshTokenTtlSeconds;

    @Column(name = "reuse_refresh_tokens", nullable = false)
    private boolean reuseRefreshTokens;

    @ElementCollection
    @CollectionTable(name = "uaa_oauth_client_auth_method", joinColumns = @JoinColumn(name = "oauth_client_id"))
    @Column(name = "authentication_method", nullable = false, length = 100)
    private Set<String> authenticationMethods = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "uaa_oauth_client_grant_type", joinColumns = @JoinColumn(name = "oauth_client_id"))
    @Column(name = "grant_type", nullable = false, length = 100)
    private Set<String> grantTypes = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "uaa_oauth_client_redirect_uri", joinColumns = @JoinColumn(name = "oauth_client_id"))
    @Column(name = "redirect_uri", nullable = false, length = 1000)
    private Set<String> redirectUris = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "uaa_oauth_client_post_logout_uri", joinColumns = @JoinColumn(name = "oauth_client_id"))
    @Column(name = "post_logout_redirect_uri", nullable = false, length = 1000)
    private Set<String> postLogoutRedirectUris = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "uaa_oauth_client_scope", joinColumns = @JoinColumn(name = "oauth_client_id"))
    @Column(name = "scope", nullable = false, length = 200)
    private Set<String> scopes = new LinkedHashSet<>();

    public OAuthClientEntity(RegisteredClient client) {
        this.id = client.getId();
        update(client);
    }

    public void update(RegisteredClient client) {
        this.clientId = client.getClientId();
        this.clientIdIssuedAt = client.getClientIdIssuedAt();
        this.clientSecret = client.getClientSecret();
        this.clientSecretExpiresAt = client.getClientSecretExpiresAt();
        this.clientName = client.getClientName();
        this.enabled = true;
        this.requireProofKey = client.getClientSettings().isRequireProofKey();
        this.requireConsent = client.getClientSettings().isRequireAuthorizationConsent();
        this.accessTokenTtlSeconds = client.getTokenSettings().getAccessTokenTimeToLive().toSeconds();
        this.refreshTokenTtlSeconds = client.getTokenSettings().getRefreshTokenTimeToLive().toSeconds();
        this.reuseRefreshTokens = client.getTokenSettings().isReuseRefreshTokens();
        replace(authenticationMethods, client.getClientAuthenticationMethods().stream()
                .map(method -> method.getValue()).toList());
        replace(grantTypes, client.getAuthorizationGrantTypes().stream()
                .map(type -> type.getValue()).toList());
        replace(redirectUris, client.getRedirectUris());
        replace(postLogoutRedirectUris, client.getPostLogoutRedirectUris());
        replace(scopes, client.getScopes());
    }

    private static void replace(Set<String> target, Iterable<String> values) {
        target.clear();
        values.forEach(target::add);
    }
}
