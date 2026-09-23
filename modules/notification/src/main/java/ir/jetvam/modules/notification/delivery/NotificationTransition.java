package ir.jetvam.modules.notification.delivery;

import ir.jetvam.modules.notification.model.NotificationStatus;

/**
 * Reports the durable state reached after a delivery attempt.
 * Dispatch telemetry uses it without reopening or exposing the persisted payload.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record NotificationTransition(NotificationStatus status, int attemptCount, String errorCode) {
}
