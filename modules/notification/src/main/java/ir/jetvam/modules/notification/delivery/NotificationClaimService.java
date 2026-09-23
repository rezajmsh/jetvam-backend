package ir.jetvam.modules.notification.delivery;

import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.modules.notification.config.NotificationProperties;
import ir.jetvam.modules.notification.model.NotificationStatus;
import ir.jetvam.modules.notification.persistence.NotificationOutboxEntity;
import ir.jetvam.modules.notification.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Claims due outbox records under a pessimistic database lock.
 * Expired messages are terminated before sensitive provider calls occur.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Service
@RequiredArgsConstructor
public class NotificationClaimService {

    private static final List<NotificationStatus> CLAIMABLE = List.of(
            NotificationStatus.PENDING,
            NotificationStatus.RETRY,
            NotificationStatus.PROCESSING
    );

    private final NotificationOutboxRepository repository;
    private final NotificationProperties properties;
    private final TimeProvider timeProvider;

    @Transactional
    public Optional<ClaimedNotification> claimNext() {
        Instant now = timeProvider.now();
        List<NotificationOutboxEntity> candidates = repository
                .findTop100ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(CLAIMABLE, now);
        for (NotificationOutboxEntity candidate : candidates) {
            Optional<ClaimedNotification> claimed = claim(candidate, now);
            if (claimed.isPresent()) {
                return claimed;
            }
        }
        return Optional.empty();
    }

    private Optional<ClaimedNotification> claim(NotificationOutboxEntity candidate, Instant now) {
        NotificationOutboxEntity locked = repository.findByIdForUpdate(candidate.getId()).orElse(null);
        if (locked == null || !locked.canBeClaimed(now)) {
            return Optional.empty();
        }
        if (locked.isExpired(now)) {
            locked.markDead("EXPIRED");
            repository.save(locked);
            return Optional.empty();
        }
        locked.claim(now.plus(properties.getOutbox().getLeaseDuration()));
        repository.saveAndFlush(locked);
        return Optional.of(new ClaimedNotification(
                locked.getId(),
                locked.getChannel(),
                locked.getTemplateCode(),
                locked.getEncryptedPayload(),
                locked.getAttemptCount()
        ));
    }
}
