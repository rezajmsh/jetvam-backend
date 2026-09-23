package ir.jetvam.modules.notification.delivery;

/**
 * Captures the optional provider identifier returned after successful delivery.
 * The identifier supports operational reconciliation without exposing message content.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record NotificationDeliveryResult(String providerMessageId) {
}
