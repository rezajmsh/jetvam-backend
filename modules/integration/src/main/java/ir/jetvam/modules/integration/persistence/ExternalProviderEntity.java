package ir.jetvam.modules.integration.persistence;

import ir.jetvam.common.text.TextUtils;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import ir.jetvam.modules.integration.model.ProviderAuthenticationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.util.Set;

/**
 * Describes one deploy-independent provider binding for an external capability.
 * Credentials are referenced indirectly and never persisted as plaintext values.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Entity
@Table(
        name = "integration_provider",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_integration_provider_capability_code",
                columnNames = {"capability_code", "provider_code"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalProviderEntity extends AbstractAuditableUuidEntity {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    @Column(name = "capability_code", nullable = false, length = 100)
    private String capabilityCode;

    @Column(name = "provider_code", nullable = false, length = 100)
    private String providerCode;

    @Column(name = "adapter_code", nullable = false, length = 120)
    private String adapterCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "weight", nullable = false)
    private int weight;

    @Column(name = "base_url", nullable = false, length = 1000)
    private String baseUrl;

    @Column(name = "operation_path", nullable = false, length = 500)
    private String operationPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "authentication_type", nullable = false, length = 30)
    private ProviderAuthenticationType authenticationType;

    @Column(name = "authentication_header", length = 150)
    private String authenticationHeader;

    @Column(name = "authentication_username", length = 250)
    private String authenticationUsername;

    @Column(name = "credential_secret_ref", length = 1000)
    private String credentialSecretRef;

    @Column(name = "tls_profile_code", length = 100)
    private String tlsProfileCode;

    @Column(name = "connect_timeout_ms", nullable = false)
    private long connectTimeoutMillis;

    @Column(name = "read_timeout_ms", nullable = false)
    private long readTimeoutMillis;

    @Column(name = "metadata_json", nullable = false, columnDefinition = "text")
    private String metadataJson;

    public ExternalProviderEntity(String capabilityCode, String providerCode) {
        this.capabilityCode = normalize(capabilityCode, "capabilityCode");
        this.providerCode = normalize(providerCode, "providerCode");
    }

    public void configure(
            String adapterCode,
            boolean enabled,
            int priority,
            int weight,
            String baseUrl,
            String operationPath,
            ProviderAuthenticationType authenticationType,
            String authenticationHeader,
            String authenticationUsername,
            String credentialSecretRef,
            String tlsProfileCode,
            long connectTimeoutMillis,
            long readTimeoutMillis,
            String metadataJson
    ) {
        this.adapterCode = normalize(adapterCode, "adapterCode");
        this.enabled = enabled;
        this.priority = Preconditions.requireNonNegative(priority, "priority");
        this.weight = Preconditions.requirePositive(weight, "weight");
        this.baseUrl = validateBaseUrl(baseUrl);
        this.operationPath = validatePath(operationPath);
        this.authenticationType = Preconditions.requireNonNull(authenticationType, "authenticationType");
        this.authenticationHeader = stripToNull(authenticationHeader);
        this.authenticationUsername = stripToNull(authenticationUsername);
        this.credentialSecretRef = stripToNull(credentialSecretRef);
        this.tlsProfileCode = TextUtils.hasText(tlsProfileCode) ? normalize(tlsProfileCode, "tlsProfileCode") : null;
        this.connectTimeoutMillis = Preconditions.requirePositive(connectTimeoutMillis, "connectTimeoutMillis");
        this.readTimeoutMillis = Preconditions.requirePositive(readTimeoutMillis, "readTimeoutMillis");
        this.metadataJson = TextUtils.hasText(metadataJson) ? metadataJson.strip() : "{}";
        validateAuthentication();
    }

    private void validateAuthentication() {
        switch (authenticationType) {
            case NONE -> { }
            case API_KEY -> {
                Preconditions.requireText(authenticationHeader, "authenticationHeader");
                Preconditions.requireText(credentialSecretRef, "credentialSecretRef");
            }
            case BASIC -> {
                Preconditions.requireText(authenticationUsername, "authenticationUsername");
                Preconditions.requireText(credentialSecretRef, "credentialSecretRef");
            }
            case BEARER, CUSTOM -> Preconditions.requireText(credentialSecretRef, "credentialSecretRef");
        }
    }

    private static String validateBaseUrl(String value) {
        URI uri = URI.create(Preconditions.requireText(value, "baseUrl").strip());
        Preconditions.require(uri.isAbsolute() && ALLOWED_SCHEMES.contains(uri.getScheme().toLowerCase()),
                "Provider base URL must use http or https");
        Preconditions.require(uri.getRawUserInfo() == null && uri.getRawQuery() == null && uri.getRawFragment() == null,
                "Provider base URL must not contain credentials, query or fragment");
        return uri.toString();
    }

    private static String validatePath(String value) {
        String path = Preconditions.requireText(value, "operationPath").strip();
        Preconditions.require(path.startsWith("/"), "Provider operation path must start with /");
        return path;
    }

    private static String normalize(String value, String name) {
        return Preconditions.requireText(value, name).strip().toUpperCase();
    }

    private static String stripToNull(String value) {
        return TextUtils.hasText(value) ? value.strip() : null;
    }
}
