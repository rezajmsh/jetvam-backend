package ir.jetvam.modules.notification.service;

/**
 * Defines the reusable entry point for durable outbound notifications.
 * Business modules depend on this abstraction instead of concrete providers.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public interface NotificationService {

    NotificationReceipt enqueue(NotificationCommand command);

    void cancel(String idempotencyKey);
}
