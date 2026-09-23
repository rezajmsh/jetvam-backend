package ir.jetvam.apps.uaa.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import ir.jetvam.common.exception.ConfigurationException;
import ir.jetvam.common.text.TextUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.cert.Certificate;
import java.util.UUID;

/**
 * Loads a stable RSA JWT signing key from a deployment-managed keystore.
 * Local environments may explicitly fall back to an ephemeral development key.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Configuration(proxyBeanMethods = false)
public class JwtSigningKeyConfiguration {

    @Bean
    JWKSource<SecurityContext> jetvamJwkSource(
            JetvamUaaProperties properties,
            ResourceLoader resourceLoader
    ) {
        RSAKey rsaKey = TextUtils.hasText(properties.getSigningKey().getKeyStoreLocation())
                ? loadKey(properties.getSigningKey(), resourceLoader)
                : ephemeralKey(properties.getSigningKey());
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    private static RSAKey loadKey(
            JetvamUaaProperties.SigningKey properties,
            ResourceLoader resourceLoader
    ) {
        try {
            Resource resource = resourceLoader.getResource(properties.getKeyStoreLocation());
            KeyStore keyStore = KeyStore.getInstance(properties.getKeyStoreType());
            char[] storePassword = chars(properties.getKeyStorePassword());
            try (InputStream input = resource.getInputStream()) {
                keyStore.load(input, storePassword);
            }
            char[] keyPassword = TextUtils.hasText(properties.getKeyPassword())
                    ? properties.getKeyPassword().toCharArray()
                    : storePassword;
            RSAPrivateKey privateKey = (RSAPrivateKey) keyStore.getKey(properties.getKeyAlias(), keyPassword);
            Certificate certificate = keyStore.getCertificate(properties.getKeyAlias());
            RSAPublicKey publicKey = (RSAPublicKey) certificate.getPublicKey();
            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(properties.getKeyAlias())
                    .build();
        } catch (Exception exception) {
            throw new ConfigurationException(
                    "jetvam.uaa.signing-key.key-store-location",
                    "Unable to load the UAA JWT signing key",
                    exception
            );
        }
    }

    private static RSAKey ephemeralKey(JetvamUaaProperties.SigningKey properties) {
        if (!properties.isAllowEphemeral()) {
            throw new ConfigurationException(
                    "jetvam.uaa.signing-key.key-store-location is required when ephemeral keys are disabled"
            );
        }
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var keyPair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception exception) {
            throw new ConfigurationException("Unable to generate a local JWT signing key", exception);
        }
    }

    private static char[] chars(String value) {
        return value == null ? new char[0] : value.toCharArray();
    }
}
