package ir.jetvam.modules.notification.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies fail-fast checks for encryption and external SMS configuration.
 * Disabled facilities remain usable in applications that only consume module APIs.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
class NotificationConfigurationValidatorTest {

    @Test
    void acceptsDisabledDispatcherWithoutEncryptionSecret() {
        NotificationProperties properties = new NotificationProperties();

        assertThatCode(() -> new NotificationConfigurationValidator(properties).afterPropertiesSet())
                .doesNotThrowAnyException();
    }

    @Test
    void requiresStrongEncryptionKeyForEnabledDispatcher() {
        NotificationProperties properties = new NotificationProperties();
        properties.getDispatcher().setEnabled(true);
        properties.getOutbox().setEncryptionKeyBase64(Base64.getEncoder().encodeToString(
                "too-short".getBytes(StandardCharsets.UTF_8)
        ));

        assertThatThrownBy(() -> new NotificationConfigurationValidator(properties).afterPropertiesSet())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification encryption key must contain exactly 32 bytes");
    }

    @Test
    void rejectsCredentialBearingProviderUrls() {
        NotificationProperties properties = new NotificationProperties();
        properties.getSms().setEnabled(true);
        properties.getSms().setBaseUrl("https://user:secret@sms.example.test");

        assertThatThrownBy(() -> new NotificationConfigurationValidator(properties).afterPropertiesSet())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Notification SMS base URL must not contain credentials, query or fragment");
    }
}
