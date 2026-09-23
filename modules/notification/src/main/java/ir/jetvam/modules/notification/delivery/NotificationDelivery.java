package ir.jetvam.modules.notification.delivery;

import ir.jetvam.modules.notification.model.NotificationChannel;

import java.util.Map;
import java.util.UUID;

/**
 * Supplies decrypted, provider-neutral data to a channel delivery adapter.
 * Sensitive values are short-lived and are never included in infrastructure logs.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record NotificationDelivery(
        UUID notificationId,
        NotificationChannel channel,
        String destination,
        String templateCode,
        Map<String, String> parameters
) {
}
