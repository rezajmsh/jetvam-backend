package ir.jetvam.modules.notification.delivery;

import ir.jetvam.modules.notification.model.NotificationChannel;

import java.util.UUID;

/**
 * Represents an outbox item exclusively leased by the current dispatcher.
 * Its encrypted payload remains opaque until the delivery boundary needs it.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record ClaimedNotification(
        UUID id,
        NotificationChannel channel,
        String templateCode,
        String encryptedPayload,
        int attemptCount
) {
}
