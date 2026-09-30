package ir.jetvam.modules.notification.integration;

import ir.jetvam.modules.notification.delivery.NotificationDelivery;
import ir.jetvam.modules.notification.model.NotificationChannel;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that local SMS delivery remains behind the production provider contract.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
class MockSmsAdapterTest {

    @Test
    void returnsAStableProviderMessageIdentifier() {
        UUID notificationId = UUID.randomUUID();
        MockSmsAdapter adapter = new MockSmsAdapter();

        var result = adapter.execute(new NotificationDelivery(
                notificationId, NotificationChannel.SMS, "09121234567", "APPLICATION_UPDATED", Map.of()
        ), null);

        assertThat(adapter.capabilityCode()).isEqualTo(SmsIntegrationCapabilities.SMS_SEND);
        assertThat(result.providerMessageId()).isEqualTo("MOCK-SMS-" + notificationId);
    }
}
