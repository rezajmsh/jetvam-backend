package ir.jetvam.modules.integration.http;

import ir.jetvam.infra.http.JetvamHttpClientFactory;
import ir.jetvam.modules.integration.model.ProviderAuthenticationType;
import ir.jetvam.modules.integration.routing.ProviderInvocationContext;
import ir.jetvam.modules.integration.security.ProviderSecretResolver;
import ir.jetvam.modules.integration.service.ExternalProviderView;
import ir.jetvam.modules.integration.service.TlsProfileView;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Builds and caches outbound clients from versioned provider, authentication and TLS configuration.
 * Database updates and file rotations change the fingerprint and replace clients without redeployment.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Component
public class DynamicProviderHttpClientFactory {

    private final JetvamHttpClientFactory clientFactory;
    private final ProviderSecretResolver secretResolver;
    private final ResourceLoader resourceLoader;
    private final Map<String, CachedClient> clients = new ConcurrentHashMap<>();

    public DynamicProviderHttpClientFactory(
            JetvamHttpClientFactory clientFactory,
            ProviderSecretResolver secretResolver,
            ResourceLoader resourceLoader
    ) {
        this.clientFactory = clientFactory;
        this.secretResolver = secretResolver;
        this.resourceLoader = resourceLoader;
    }

    public RestClient get(ProviderInvocationContext context) {
        ExternalProviderView provider = context.provider();
        String key = provider.capabilityCode() + ":" + provider.providerCode();
        String fingerprint = fingerprint(context);
        CachedClient current = clients.get(key);
        if (current != null && current.fingerprint().equals(fingerprint)) {
            return current.client();
        }
        return clients.compute(key, (ignored, existing) -> {
            if (existing != null && existing.fingerprint().equals(fingerprint)) {
                return existing;
            }
            return new CachedClient(fingerprint, build(context));
        }).client();
    }

    public void invalidateAll() {
        clients.clear();
    }

    private RestClient build(ProviderInvocationContext context) {
        ExternalProviderView provider = context.provider();
        SslMaterial ssl = context.tlsProfile().map(this::sslMaterial).orElse(SslMaterial.none());
        return clientFactory.create(
                "integration-" + provider.capabilityCode().toLowerCase() + "-"
                        + provider.providerCode().toLowerCase(),
                URI.create(provider.baseUrl()),
                Duration.ofMillis(provider.connectTimeoutMillis()),
                Duration.ofMillis(provider.readTimeoutMillis()),
                ssl.context(),
                ssl.enabledProtocols(),
                authentication(provider)
        );
    }

    private Consumer<HttpHeaders> authentication(ExternalProviderView provider) {
        return headers -> {
            ProviderAuthenticationType type = provider.authenticationType();
            if (type == ProviderAuthenticationType.API_KEY) {
                headers.set(provider.authenticationHeader(), secretResolver.resolve(provider.credentialSecretRef()));
            } else if (type == ProviderAuthenticationType.BASIC) {
                headers.setBasicAuth(provider.authenticationUsername(),
                        secretResolver.resolve(provider.credentialSecretRef()));
            } else if (type == ProviderAuthenticationType.BEARER) {
                headers.setBearerAuth(secretResolver.resolve(provider.credentialSecretRef()));
            }
        };
    }

    private SslMaterial sslMaterial(TlsProfileView profile) {
        try {
            TrustManager[] trustManagers = trustManagers(profile);
            KeyManager[] keyManagers = keyManagers(profile);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
            String[] protocols = Arrays.stream(profile.enabledProtocols().split(","))
                    .map(String::strip).filter(value -> !value.isEmpty()).toArray(String[]::new);
            return new SslMaterial(sslContext, protocols);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not initialize provider TLS profile " + profile.profileCode(),
                    exception);
        }
    }

    private TrustManager[] trustManagers(TlsProfileView profile) throws Exception {
        if (profile.trustStoreLocation() == null) {
            return null;
        }
        char[] password = secretChars(profile.trustStorePasswordRef());
        KeyStore store = loadStore(profile.storeType(), profile.trustStoreLocation(), password);
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(store);
        return factory.getTrustManagers();
    }

    private KeyManager[] keyManagers(TlsProfileView profile) throws Exception {
        if (profile.keyStoreLocation() == null) {
            return null;
        }
        char[] storePassword = secretChars(profile.keyStorePasswordRef());
        char[] keyPassword = profile.keyPasswordRef() == null
                ? storePassword : secretChars(profile.keyPasswordRef());
        KeyStore store = loadStore(profile.storeType(), profile.keyStoreLocation(), storePassword);
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(store, keyPassword);
        return factory.getKeyManagers();
    }

    private KeyStore loadStore(String type, String location, char[] password) throws Exception {
        KeyStore store = KeyStore.getInstance(type);
        Resource resource = resourceLoader.getResource(location);
        try (InputStream input = resource.getInputStream()) {
            store.load(input, password);
        }
        return store;
    }

    private char[] secretChars(String reference) {
        return reference == null ? null : secretResolver.resolve(reference).toCharArray();
    }

    private String fingerprint(ProviderInvocationContext context) {
        ExternalProviderView provider = context.provider();
        StringBuilder value = new StringBuilder()
                .append(provider.version()).append(':').append(provider.baseUrl()).append(':')
                .append(provider.authenticationType());
        if (provider.credentialSecretRef() != null
                && provider.authenticationType() != ProviderAuthenticationType.CUSTOM) {
            value.append(':').append(secretResolver.fingerprint(provider.credentialSecretRef()));
        }
        context.tlsProfile().ifPresent(profile -> value.append(":tls:").append(profile.version())
                .append(':').append(resourceFingerprint(profile.trustStoreLocation()))
                .append(':').append(resourceFingerprint(profile.keyStoreLocation()))
                .append(':').append(secretFingerprint(profile.trustStorePasswordRef()))
                .append(':').append(secretFingerprint(profile.keyStorePasswordRef()))
                .append(':').append(secretFingerprint(profile.keyPasswordRef())));
        return value.toString();
    }

    private String resourceFingerprint(String location) {
        if (location == null) {
            return "-";
        }
        try {
            Resource resource = resourceLoader.getResource(location);
            return location + ":" + resource.lastModified() + ":" + resource.contentLength();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not inspect provider TLS material", exception);
        }
    }

    private String secretFingerprint(String reference) {
        return reference == null ? "-" : secretResolver.fingerprint(reference);
    }

    private record CachedClient(String fingerprint, RestClient client) {
    }

    private record SslMaterial(SSLContext context, String[] enabledProtocols) {
        private static SslMaterial none() {
            return new SslMaterial(null, new String[0]);
        }
    }
}
