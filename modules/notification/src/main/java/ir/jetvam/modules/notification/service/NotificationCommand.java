package ir.jetvam.modules.notification.service;

import ir.jetvam.modules.notification.model.NotificationChannel;

import java.time.Instant;
import java.util.Map;

/**
 * Carries a channel-neutral request to the durable notification boundary.
 * The idempotency key prevents duplicate messages during producer retries.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record NotificationCommand(
        NotificationChannel channel,
        String destination,
        String templateCode,
        Map<String, String> parameters,
        String idempotencyKey,
        Instant deliverAfter,
        Instant expiresAt
) {
}
