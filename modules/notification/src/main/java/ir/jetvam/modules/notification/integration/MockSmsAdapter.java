package ir.jetvam.modules.notification.integration;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.integration.routing.ExternalProviderAdapter;
import ir.jetvam.modules.integration.routing.ProviderInvocationContext;
import ir.jetvam.modules.notification.delivery.NotificationDelivery;
import ir.jetvam.modules.notification.delivery.NotificationDeliveryResult;
import org.springframework.stereotype.Component;

/**
 * Delivers SMS messages in process for local development while preserving the provider-routing contract.
 * Message content and destination are intentionally not logged.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Component
public final class MockSmsAdapter implements ExternalProviderAdapter<NotificationDelivery, NotificationDeliveryResult> {

    public static final String ADAPTER_CODE = "SMS_MOCK_V1";

    @Override public String capabilityCode() { return SmsIntegrationCapabilities.SMS_SEND; }
    @Override public String adapterCode() { return ADAPTER_CODE; }
    @Override public Class<NotificationDelivery> commandType() { return NotificationDelivery.class; }

    @Override
    public NotificationDeliveryResult execute(NotificationDelivery command, ProviderInvocationContext context) {
        Preconditions.requireNonNull(command, "command");
        return new NotificationDeliveryResult("MOCK-SMS-" + command.notificationId());
    }
}
