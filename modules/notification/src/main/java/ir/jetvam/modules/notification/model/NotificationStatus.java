package ir.jetvam.modules.notification.model;

/**
 * Describes the durable delivery lifecycle of an outbound notification.
 * Terminal states distinguish successful delivery from exhausted retries.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public enum NotificationStatus {
    PENDING,
    PROCESSING,
    RETRY,
    SENT,
    DEAD
}
