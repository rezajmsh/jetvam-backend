package ir.jetvam.modules.notification.config;

import ir.jetvam.common.validation.Preconditions;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Base64;

/**
 * Fails application startup when enabled notification facilities are unsafe or incomplete.
 * Validation keeps encryption, polling and provider failures out of runtime traffic paths.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Component
@RequiredArgsConstructor
public class NotificationConfigurationValidator implements InitializingBean {

    private final NotificationProperties properties;

    @Override
    public void afterPropertiesSet() {
        validateOutbox();
        validateDispatcher();
    }

    private void validateOutbox() {
        NotificationProperties.Outbox outbox = properties.getOutbox();
        Preconditions.requirePositive(outbox.getMaxAttempts(), "jetvam.notification.outbox.max-attempts");
        requirePositive(outbox.getInitialRetryDelay(), "jetvam.notification.outbox.initial-retry-delay");
        requirePositive(outbox.getMaximumRetryDelay(), "jetvam.notification.outbox.maximum-retry-delay");
        requirePositive(outbox.getLeaseDuration(), "jetvam.notification.outbox.lease-duration");
        Preconditions.require(
                outbox.getMaximumRetryDelay().compareTo(outbox.getInitialRetryDelay()) >= 0,
                "Notification maximum retry delay must not be shorter than initial retry delay"
        );
        if (properties.getDispatcher().isEnabled()) {
            validateEncryptionKey(outbox.getEncryptionKeyBase64());
        }
    }

    private void validateDispatcher() {
        NotificationProperties.Dispatcher dispatcher = properties.getDispatcher();
        Preconditions.requirePositive(dispatcher.getBatchSize(), "jetvam.notification.dispatcher.batch-size");
        requirePositive(dispatcher.getFixedDelay(), "jetvam.notification.dispatcher.fixed-delay");
    }

    private static void validateEncryptionKey(String encodedKey) {
        String value = Preconditions.requireText(
                encodedKey,
                "jetvam.notification.outbox.encryption-key-base64"
        );
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Notification encryption key must be valid Base64", exception);
        }
        Preconditions.require(decoded.length == 32, "Notification encryption key must contain exactly 32 bytes");
    }

    private static void requirePositive(Duration duration, String propertyName) {
        Preconditions.requireNonNull(duration, propertyName);
        Preconditions.require(!duration.isZero() && !duration.isNegative(), propertyName + " must be positive");
    }
}
