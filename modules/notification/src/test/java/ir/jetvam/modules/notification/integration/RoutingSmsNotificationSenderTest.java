package ir.jetvam.modules.notification.integration;

import ir.jetvam.modules.integration.routing.ProviderInvocationException;
import ir.jetvam.modules.integration.routing.ProviderRouter;
import ir.jetvam.modules.notification.delivery.NotificationDelivery;
import ir.jetvam.modules.notification.delivery.NotificationDeliveryException;
import ir.jetvam.modules.notification.delivery.NotificationDeliveryResult;
import ir.jetvam.modules.notification.model.NotificationChannel;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies mapping between notification delivery semantics and provider-router semantics.
 * The tests also protect the no-failover rule for ambiguous SMS writes.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
class RoutingSmsNotificationSenderTest {

    private final ProviderRouter providerRouter = mock(ProviderRouter.class);
    private final RoutingSmsNotificationSender sender = new RoutingSmsNotificationSender(providerRouter);

    @Test
    void routesSmsWithoutAllowingAmbiguousFailover() {
        NotificationDelivery delivery = delivery();
        NotificationDeliveryResult expected = new NotificationDeliveryResult("provider-message-id");
        when(providerRouter.execute(
                SmsIntegrationCapabilities.SMS_SEND,
                delivery,
                NotificationDeliveryResult.class,
                false
        )).thenReturn(expected);

        NotificationDeliveryResult actual = sender.send(delivery);

        assertThat(actual).isSameAs(expected);
        verify(providerRouter).execute(
                SmsIntegrationCapabilities.SMS_SEND,
                delivery,
                NotificationDeliveryResult.class,
                false
        );
    }

    @Test
    void preservesNormalizedProviderFailureClassification() {
        NotificationDelivery delivery = delivery();
        ProviderInvocationException providerFailure = ProviderInvocationException.retryable(
                "HTTP_503", false, new IllegalStateException("provider failed")
        );
        when(providerRouter.execute(
                SmsIntegrationCapabilities.SMS_SEND,
                delivery,
                NotificationDeliveryResult.class,
                false
        )).thenThrow(providerFailure);

        assertThatThrownBy(() -> sender.send(delivery))
                .isInstanceOfSatisfying(NotificationDeliveryException.class, failure -> {
                    assertThat(failure.getErrorCode()).isEqualTo("HTTP_503");
                    assertThat(failure.isRetryable()).isTrue();
                    assertThat(failure.getCause()).isSameAs(providerFailure);
                });
    }

    private static NotificationDelivery delivery() {
        return new NotificationDelivery(
                UUID.randomUUID(), NotificationChannel.SMS, "09120000000", "OTP",
                Map.of("otp", "123456")
        );
    }
}
