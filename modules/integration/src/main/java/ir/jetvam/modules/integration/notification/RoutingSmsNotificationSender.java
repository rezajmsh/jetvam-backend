package ir.jetvam.modules.integration.notification;

import ir.jetvam.modules.integration.IntegrationCapabilities;
import ir.jetvam.modules.integration.routing.ProviderInvocationException;
import ir.jetvam.modules.integration.routing.ProviderRouter;
import ir.jetvam.modules.notification.delivery.NotificationChannelSender;
import ir.jetvam.modules.notification.delivery.NotificationDelivery;
import ir.jetvam.modules.notification.delivery.NotificationDeliveryException;
import ir.jetvam.modules.notification.delivery.NotificationDeliveryResult;
import ir.jetvam.modules.notification.model.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Routes SMS delivery across configured providers while preserving notification semantics.
 * Ambiguous send failures do not fail over because a second provider could duplicate a message.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Component
@RequiredArgsConstructor
public class RoutingSmsNotificationSender implements NotificationChannelSender {

    private final ProviderRouter providerRouter;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public NotificationDeliveryResult send(NotificationDelivery delivery) {
        try {
            return providerRouter.execute(
                    IntegrationCapabilities.SMS_SEND,
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
