package ir.jetvam.modules.notification.delivery;

import ir.jetvam.modules.notification.model.NotificationChannel;

/**
 * Defines the provider adapter contract for one notification channel.
 * Queue orchestration remains independent from SMS, email and push transports.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public interface NotificationChannelSender {

    NotificationChannel channel();

    NotificationDeliveryResult send(NotificationDelivery delivery);
}
