package ir.jetvam.modules.integration.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies safe environment and hot-rotatable file secret resolution.
 * Inline secret values are rejected so database configuration cannot hold credentials.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
class DefaultProviderSecretResolverTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void resolvesEnvironmentAndFileReferences() throws Exception {
        MockEnvironment environment = new MockEnvironment().withProperty("SMS_TOKEN", "environment-secret");
        DefaultProviderSecretResolver resolver = new DefaultProviderSecretResolver(environment);
        Path file = temporaryDirectory.resolve("provider.secret");
        Files.writeString(file, "first-secret\n");

        assertThat(resolver.resolve("env:SMS_TOKEN")).isEqualTo("environment-secret");
        assertThat(resolver.resolve("file:" + file)).isEqualTo("first-secret");

        String firstFingerprint = resolver.fingerprint("file:" + file);
        Files.writeString(file, "rotated-secret-value");
        assertThat(resolver.fingerprint("file:" + file)).isNotEqualTo(firstFingerprint);
        assertThat(resolver.resolve("file:" + file)).isEqualTo("rotated-secret-value");
    }

    @Test
    void rejectsInlineSecrets() {
        DefaultProviderSecretResolver resolver = new DefaultProviderSecretResolver(new MockEnvironment());

        assertThatThrownBy(() -> resolver.resolve("plain-secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported secret reference scheme");
    }
}
