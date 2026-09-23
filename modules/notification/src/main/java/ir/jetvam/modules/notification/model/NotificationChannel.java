package ir.jetvam.modules.notification.model;

/**
 * Identifies the delivery medium selected for an outbound notification.
 * New channels can be added without changing notification producers.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public enum NotificationChannel {
    SMS,
    EMAIL,
    PUSH,
    IN_APP
}
