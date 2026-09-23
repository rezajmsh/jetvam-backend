package ir.jetvam.modules.notification.delivery;

import ir.jetvam.modules.notification.config.NotificationProperties;
import ir.jetvam.modules.notification.model.NotificationChannel;
import ir.jetvam.modules.notification.model.NotificationStatus;
import ir.jetvam.modules.notification.observability.NotificationTelemetry;
import ir.jetvam.modules.notification.security.NotificationPayload;
import ir.jetvam.modules.notification.security.NotificationPayloadCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies delivery orchestration maps permanent and transient failures correctly.
 * Provider classification must reach durable state transitions without payload leakage.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
class NotificationDeliveryServiceTest {

    private NotificationClaimService claimService;
    private NotificationStateService stateService;
    private NotificationPayloadCipher payloadCipher;
    private NotificationChannelSender sender;
    private NotificationTelemetry telemetry;
    private ClaimedNotification claimed;

    @BeforeEach
    void setUp() {
        claimService = mock(NotificationClaimService.class);
        stateService = mock(NotificationStateService.class);
        payloadCipher = mock(NotificationPayloadCipher.class);
        sender = mock(NotificationChannelSender.class);
        telemetry = mock(NotificationTelemetry.class);
        claimed = new ClaimedNotification(
                UUID.randomUUID(),
                NotificationChannel.SMS,
                "identity.otp.customer-login",
                "encrypted",
                1
        );
        when(claimService.claimNext()).thenReturn(Optional.of(claimed));
        when(payloadCipher.decrypt("encrypted"))
                .thenReturn(new NotificationPayload("09121234567", Map.of("code", "123456")));
        when(sender.channel()).thenReturn(NotificationChannel.SMS);
    }

    @Test
    void marksPermanentProviderRejectionAsDead() {
        when(sender.send(org.mockito.ArgumentMatchers.any()))
                .thenThrow(NotificationDeliveryException.permanent("HTTP_400", new IllegalStateException()));
        NotificationTransition transition = new NotificationTransition(NotificationStatus.DEAD, 1, "HTTP_400");
        when(stateService.markFailed(claimed.id(), "HTTP_400", false)).thenReturn(transition);

        NotificationBatchResult result = service().dispatchBatch();

        assertEquals(new NotificationBatchResult(1, 0, 1), result);
        verify(stateService).markFailed(claimed.id(), "HTTP_400", false);
        verify(telemetry).failed(
                org.mockito.ArgumentMatchers.eq(claimed),
                org.mockito.ArgumentMatchers.eq(transition),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void schedulesRetryForTransientProviderFailure() {
        when(sender.send(org.mockito.ArgumentMatchers.any()))
                .thenThrow(NotificationDeliveryException.retryable("HTTP_503", new IllegalStateException()));
        NotificationTransition transition = new NotificationTransition(NotificationStatus.RETRY, 1, "HTTP_503");
        when(stateService.markFailed(claimed.id(), "HTTP_503", true)).thenReturn(transition);

        NotificationBatchResult result = service().dispatchBatch();

        assertEquals(new NotificationBatchResult(1, 0, 1), result);
        verify(stateService).markFailed(claimed.id(), "HTTP_503", true);
    }

    @Test
    void reportsSuccessfulAndFailedItemCounters() {
        when(sender.send(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new NotificationDeliveryResult("provider-message"));
        NotificationTransition transition = new NotificationTransition(NotificationStatus.SENT, 1, null);
        when(stateService.markSent(claimed.id(), "provider-message")).thenReturn(transition);

        NotificationBatchResult result = service().dispatchBatch();

        assertEquals(new NotificationBatchResult(1, 1, 0), result);
        verify(stateService).markSent(claimed.id(), "provider-message");
    }

    private NotificationDeliveryService service() {
        NotificationProperties properties = new NotificationProperties();
        properties.getDispatcher().setBatchSize(1);
        return new NotificationDeliveryService(
                claimService,
                stateService,
                payloadCipher,
                List.of(sender),
                properties,
                telemetry
        );
    }
}
