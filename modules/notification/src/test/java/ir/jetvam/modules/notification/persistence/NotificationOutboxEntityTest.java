package ir.jetvam.modules.notification.persistence;

import ir.jetvam.modules.notification.model.NotificationChannel;
import ir.jetvam.modules.notification.model.NotificationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies lease recovery, retry exhaustion and successful outbox transitions.
 * The state machine prevents lost or endlessly retried notifications.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
class NotificationOutboxEntityTest {

    private static final Instant NOW = Instant.parse("2026-09-22T05:00:00Z");

    @Test
    void retriesAfterLeaseAndStopsAtConfiguredAttemptLimit() {
        NotificationOutboxEntity entity = notification(2);

        entity.claim(NOW.plusSeconds(30));
        assertThat(entity.getStatus()).isEqualTo(NotificationStatus.PROCESSING);
        assertThat(entity.canBeClaimed(NOW.plusSeconds(10))).isFalse();

        entity.markFailed(NOW.plusSeconds(2), "TIMEOUT");
        assertThat(entity.getStatus()).isEqualTo(NotificationStatus.RETRY);
        assertThat(entity.canBeClaimed(NOW.plusSeconds(2))).isTrue();

        entity.claim(NOW.plusSeconds(32));
        entity.markFailed(NOW.plusSeconds(4), "TIMEOUT");
        assertThat(entity.getStatus()).isEqualTo(NotificationStatus.DEAD);
        assertThat(entity.getAttemptCount()).isEqualTo(2);
    }

    @Test
    void recordsSuccessfulProviderDelivery() {
        NotificationOutboxEntity entity = notification(3);
        entity.claim(NOW.plusSeconds(30));

        entity.markSent(NOW, "provider-42");

        assertThat(entity.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(entity.getSentAt()).isEqualTo(NOW);
        assertThat(entity.getProviderMessageId()).isEqualTo("provider-42");
    }

    @Test
    void cancelsQueuedButNotAlreadySentNotifications() {
        NotificationOutboxEntity queued = notification(3);
        assertThat(queued.canBeCancelled()).isTrue();
        queued.markDead("CANCELLED");
        assertThat(queued.getStatus()).isEqualTo(NotificationStatus.DEAD);

        NotificationOutboxEntity sent = notification(3);
        sent.claim(NOW.plusSeconds(30));
        sent.markSent(NOW, "provider-42");
        assertThat(sent.canBeCancelled()).isFalse();
    }

    private static NotificationOutboxEntity notification(int maxAttempts) {
        return new NotificationOutboxEntity(
                NotificationChannel.SMS,
                "identity.otp.customer-login",
                "encrypted",
                "identity-otp:42",
                maxAttempts,
                NOW,
                NOW.plusSeconds(120)
        );
    }
}
