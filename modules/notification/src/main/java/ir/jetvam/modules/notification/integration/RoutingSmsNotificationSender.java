package ir.jetvam.modules.notification.integration;

import ir.jetvam.modules.integration.routing.ProviderInvocationException;
import ir.jetvam.modules.integration.routing.ProviderRouter;
import ir.jetvam.modules.notification.delivery.NotificationChannelSender;
import ir.jetvam.modules.notification.delivery.NotificationDelivery;
import ir.jetvam.modules.notification.delivery.NotificationDeliveryException;
import ir.jetvam.modules.notification.delivery.NotificationDeliveryResult;
import ir.jetvam.modules.notification.model.NotificationChannel;
import org.springframework.stereotype.Component;

/**
 * Implements the notification SMS port by delegating provider selection to integration infrastructure.
 * Ambiguous send failures do not fail over because another attempt could duplicate the message.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Component
public class RoutingSmsNotificationSender implements NotificationChannelSender {

    private final ProviderRouter providerRouter;

    public RoutingSmsNotificationSender(ProviderRouter providerRouter) {
        this.providerRouter = providerRouter;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public NotificationDeliveryResult send(NotificationDelivery delivery) {
        try {
            return providerRouter.execute(
                    SmsIntegrationCapabilities.SMS_SEND,
                    delivery,
                    NotificationDeliveryResult.class,
                    false
            );
        } catch (ProviderInvocationException exception) {
            throw new NotificationDeliveryException(
                    exception.getErrorCode(), exception.isRetryable(), exception
            );
        } catch (RuntimeException exception) {
            throw NotificationDeliveryException.retryable("PROVIDER_UNAVAILABLE", exception);
        }
    }
}
