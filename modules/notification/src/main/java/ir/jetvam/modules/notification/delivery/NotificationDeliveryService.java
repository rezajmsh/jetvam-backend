package ir.jetvam.modules.notification.delivery;

import ir.jetvam.modules.notification.config.NotificationProperties;
import ir.jetvam.modules.notification.observability.NotificationTelemetry;
import ir.jetvam.modules.notification.security.NotificationPayload;
import ir.jetvam.modules.notification.security.NotificationPayloadCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.Duration;

/**
 * Dispatches leased notifications through their channel adapter outside a transaction.
 * Provider latency therefore never holds database locks or producer transactions open.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {

    private final NotificationClaimService claimService;
    private final NotificationStateService stateService;
    private final NotificationPayloadCipher payloadCipher;
    private final List<NotificationChannelSender> senders;
    private final NotificationProperties properties;
    private final NotificationTelemetry telemetry;

    public NotificationBatchResult dispatchBatch() {
        int batchSize = Math.max(1, properties.getDispatcher().getBatchSize());
        int processed = 0;
        int succeeded = 0;
        for (int index = 0; index < batchSize; index++) {
            ClaimedNotification claimed = claimService.claimNext().orElse(null);
            if (claimed == null) {
                break;
            }
            processed++;
            if (dispatch(claimed)) {
                succeeded++;
            }
        }
        return new NotificationBatchResult(processed, succeeded, processed - succeeded);
    }

    private boolean dispatch(ClaimedNotification claimed) {
        long startedAt = System.nanoTime();
        NotificationDeliveryResult result;
        try {
            NotificationPayload payload = decrypt(claimed);
            NotificationChannelSender sender = senders.stream()
                    .filter(candidate -> candidate.channel() == claimed.channel())
                    .findFirst()
                    .orElseThrow(() -> NotificationDeliveryException.permanent(
                            "SENDER_NOT_REGISTERED",
                            new IllegalStateException("No notification sender is registered for " + claimed.channel())
                    ));
            result = sender.send(new NotificationDelivery(
                    claimed.id(),
                    claimed.channel(),
                    payload.destination(),
                    claimed.templateCode(),
                    payload.parameters()
            ));
        } catch (NotificationDeliveryException exception) {
            NotificationTransition transition = stateService.markFailed(
                    claimed.id(),
                    exception.getErrorCode(),
                    exception.isRetryable()
            );
            telemetry.failed(claimed, transition, elapsed(startedAt));
            return false;
        } catch (RuntimeException exception) {
            NotificationTransition transition = stateService.markFailed(
                    claimed.id(),
                    "UNEXPECTED_ERROR",
                    true
            );
            telemetry.failed(claimed, transition, elapsed(startedAt));
            return false;
        }
        NotificationTransition transition = stateService.markSent(claimed.id(), result.providerMessageId());
        telemetry.sent(claimed, transition.attemptCount(), elapsed(startedAt));
        return true;
    }

    private NotificationPayload decrypt(ClaimedNotification claimed) {
        try {
            return payloadCipher.decrypt(claimed.encryptedPayload());
        } catch (RuntimeException exception) {
            throw NotificationDeliveryException.permanent("PAYLOAD_DECRYPTION_FAILED", exception);
        }
    }

    private static Duration elapsed(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt);
    }
}
