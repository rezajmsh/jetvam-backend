package ir.jetvam.modules.notification.security;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.notification.config.NotificationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Encrypts persisted notification destinations and parameters with AES-GCM.
 * Versioned binary encoding supports authenticated evolution without plaintext storage.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Component
public class NotificationPayloadCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int FORMAT_VERSION = 1;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final NotificationProperties properties;

    public NotificationPayloadCipher(NotificationProperties properties) {
        this.properties = properties;
    }

    public String encrypt(NotificationPayload payload) {
        Preconditions.requireNonNull(payload, "payload");
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            SECURE_RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] encrypted = cipher.doFinal(serialize(payload));
            byte[] envelope = new byte[1 + nonce.length + encrypted.length];
            envelope[0] = FORMAT_VERSION;
            System.arraycopy(nonce, 0, envelope, 1, nonce.length);
            System.arraycopy(encrypted, 0, envelope, 1 + nonce.length, encrypted.length);
            return Base64.getEncoder().encodeToString(envelope);
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("Unable to encrypt notification payload", exception);
        }
    }

    public NotificationPayload decrypt(String encoded) {
        Preconditions.requireText(encoded, "encryptedPayload");
        try {
            byte[] envelope = Base64.getDecoder().decode(encoded);
            if (envelope.length <= NONCE_LENGTH + 1 || envelope[0] != FORMAT_VERSION) {
                throw new IllegalArgumentException("Unsupported notification payload format");
            }
            byte[] nonce = new byte[NONCE_LENGTH];
            System.arraycopy(envelope, 1, nonce, 0, nonce.length);
            byte[] encrypted = new byte[envelope.length - 1 - nonce.length];
            System.arraycopy(envelope, 1 + nonce.length, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return deserialize(cipher.doFinal(encrypted));
        } catch (GeneralSecurityException | IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt notification payload", exception);
        }
    }

    private SecretKeySpec key() {
        String encodedKey = Preconditions.requireText(
                properties.getOutbox().getEncryptionKeyBase64(),
                "jetvam.notification.outbox.encryption-key-base64"
        );
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Notification encryption key must be valid Base64", exception);
        }
        Preconditions.require(decoded.length == 32, "Notification encryption key must contain exactly 32 bytes");
        return new SecretKeySpec(decoded, "AES");
    }

    private static byte[] serialize(NotificationPayload payload) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            writeText(output, Preconditions.requireText(payload.destination(), "destination"));
            Map<String, String> parameters = payload.parameters() == null ? Map.of() : payload.parameters();
            output.writeInt(parameters.size());
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                writeText(output, Preconditions.requireText(entry.getKey(), "parameter key"));
                writeText(output, entry.getValue() == null ? "" : entry.getValue());
            }
        }
        return bytes.toByteArray();
    }

    private static NotificationPayload deserialize(byte[] bytes) throws IOException {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes))) {
            String destination = readText(input);
            int size = input.readInt();
            if (size < 0 || size > 100) {
                throw new IOException("Invalid notification parameter count");
            }
            Map<String, String> parameters = new LinkedHashMap<>();
            for (int index = 0; index < size; index++) {
                parameters.put(readText(input), readText(input));
            }
            return new NotificationPayload(destination, Map.copyOf(parameters));
        }
    }

    private static void writeText(DataOutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readText(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0 || length > 65_536) {
            throw new IOException("Invalid notification payload field length");
        }
        return new String(input.readNBytes(length), StandardCharsets.UTF_8);
    }
}
