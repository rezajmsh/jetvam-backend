package ir.jetvam.modules.notification.security;

import ir.jetvam.modules.notification.config.NotificationProperties;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies authenticated round trips and plaintext exclusion for outbox payloads.
 * OTP values and destinations must never appear in persisted ciphertext.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
class NotificationPayloadCipherTest {

    @Test
    void encryptsAndRestoresSensitivePayload() {
        NotificationProperties properties = new NotificationProperties();
        properties.getOutbox().setEncryptionKeyBase64(Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)
        ));
        NotificationPayloadCipher cipher = new NotificationPayloadCipher(properties);
        NotificationPayload original = new NotificationPayload(
                "09121234567",
                Map.of("code", "123456", "purpose", "CUSTOMER_LOGIN")
        );

        String encrypted = cipher.encrypt(original);

        assertThat(encrypted).doesNotContain("09121234567", "123456", "CUSTOMER_LOGIN");
        assertThat(cipher.decrypt(encrypted)).isEqualTo(original);
    }
}
