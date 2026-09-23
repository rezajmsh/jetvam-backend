package ir.jetvam.modules.notification.service;

import ir.jetvam.modules.notification.model.NotificationStatus;

import java.util.UUID;

/**
 * Confirms that a notification has been accepted into the durable queue.
 * It exposes the stable identifier and current delivery state.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record NotificationReceipt(UUID notificationId, NotificationStatus status) {
}
