package ir.jetvam.modules.notification.delivery;

import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.modules.notification.config.NotificationProperties;
import ir.jetvam.modules.notification.model.NotificationStatus;
import ir.jetvam.modules.notification.persistence.NotificationOutboxEntity;
import ir.jetvam.modules.notification.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Commits terminal and retry states after a provider delivery attempt.
 * Locked updates prevent stale workers from overwriting completed records.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Service
@RequiredArgsConstructor
public class NotificationStateService {

    private final NotificationOutboxRepository repository;
    private final NotificationProperties properties;
    private final TimeProvider timeProvider;

    @Transactional
    public NotificationTransition markSent(UUID id, String providerMessageId) {
        NotificationOutboxEntity entity = processing(id);
        entity.markSent(timeProvider.now(), providerMessageId);
        repository.saveAndFlush(entity);
        return new NotificationTransition(entity.getStatus(), entity.getAttemptCount(), null);
    }

    @Transactional
    public NotificationTransition markFailed(UUID id, String errorCode, boolean retryable) {
        NotificationOutboxEntity entity = processing(id);
        Instant now = timeProvider.now();
        if (entity.isExpired(now)) {
            entity.markDead("EXPIRED");
        } else if (!retryable) {
            entity.markDead(errorCode);
        } else {
            entity.markFailed(now.plus(retryDelay(entity.getAttemptCount())), errorCode);
        }
        repository.saveAndFlush(entity);
        return new NotificationTransition(entity.getStatus(), entity.getAttemptCount(), entity.getLastError());
    }

    private NotificationOutboxEntity processing(UUID id) {
        NotificationOutboxEntity entity = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalStateException("Claimed notification no longer exists"));
        if (entity.getStatus() != NotificationStatus.PROCESSING) {
            throw new IllegalStateException("Notification is not in PROCESSING state");
        }
        return entity;
    }

    private Duration retryDelay(int attemptCount) {
        Duration initial = properties.getOutbox().getInitialRetryDelay();
        Duration maximum = properties.getOutbox().getMaximumRetryDelay();
        long multiplier = 1L << Math.min(Math.max(attemptCount - 1, 0), 20);
        Duration calculated;
        try {
            calculated = initial.multipliedBy(multiplier);
        } catch (ArithmeticException exception) {
            return maximum;
        }
        return calculated.compareTo(maximum) > 0 ? maximum : calculated;
    }

}
