package ir.jetvam.modules.notification.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.notification.model.NotificationStatus;
import ir.jetvam.modules.notification.persistence.NotificationOutboxEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides centrally instrumented persistence access to notification outbox records.
 * Derived queries avoid coupling the business module to JDBC or provider-specific SQL.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public interface NotificationOutboxRepository extends JetvamJpaRepository<NotificationOutboxEntity, UUID> {

    Optional<NotificationOutboxEntity> findByIdempotencyKey(String idempotencyKey);

    List<NotificationOutboxEntity> findTop100ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            Collection<NotificationStatus> statuses,
            Instant now
    );
}
