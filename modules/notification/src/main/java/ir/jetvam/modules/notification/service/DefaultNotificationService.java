package ir.jetvam.modules.notification.service;

import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.notification.config.NotificationProperties;
import ir.jetvam.modules.notification.persistence.NotificationOutboxEntity;
import ir.jetvam.modules.notification.repository.NotificationOutboxRepository;
import ir.jetvam.modules.notification.security.NotificationPayload;
import ir.jetvam.modules.notification.security.NotificationPayloadCipher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * Enqueues encrypted notifications in the caller's database transaction.
 * Stable idempotency keys make producer retries safe and observable.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultNotificationService implements NotificationService {

    private final NotificationOutboxRepository repository;
    private final NotificationPayloadCipher payloadCipher;
    private final NotificationProperties properties;
    private final TimeProvider timeProvider;

    @Override
    @Transactional
    public NotificationReceipt enqueue(NotificationCommand command) {
        validate(command);
        return repository.findByIdempotencyKey(command.idempotencyKey())
                .map(entity -> new NotificationReceipt(entity.getId(), entity.getStatus()))
                .orElseGet(() -> persist(command));
    }

    @Override
    @Transactional
    public void cancel(String idempotencyKey) {
        Preconditions.requireText(idempotencyKey, "idempotencyKey");
        repository.findByIdempotencyKey(idempotencyKey).ifPresent(candidate -> {
            NotificationOutboxEntity locked = repository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (locked != null && locked.canBeCancelled()) {
                locked.markDead("CANCELLED");
                repository.saveAndFlush(locked);
            }
        });
    }

    private NotificationReceipt persist(NotificationCommand command) {
        Instant now = timeProvider.now();
        Instant deliverAfter = command.deliverAfter() == null ? now : command.deliverAfter();
        int maxAttempts = Preconditions.requirePositive(
                properties.getOutbox().getMaxAttempts(),
                "jetvam.notification.outbox.max-attempts"
        );
        String encryptedPayload = payloadCipher.encrypt(new NotificationPayload(
                command.destination(),
                command.parameters() == null ? Map.of() : Map.copyOf(command.parameters())
        ));
        NotificationOutboxEntity entity = repository.saveAndFlush(new NotificationOutboxEntity(
                command.channel(),
                command.templateCode(),
                encryptedPayload,
                command.idempotencyKey(),
                maxAttempts,
                deliverAfter,
                command.expiresAt()
        ));
        return new NotificationReceipt(entity.getId(), entity.getStatus());
    }

    private static void validate(NotificationCommand command) {
        Preconditions.requireNonNull(command, "command");
        Preconditions.requireNonNull(command.channel(), "channel");
        Preconditions.requireText(command.destination(), "destination");
        Preconditions.requireText(command.templateCode(), "templateCode");
        Preconditions.requireText(command.idempotencyKey(), "idempotencyKey");
        Preconditions.require(command.destination().length() <= 512,
                "Notification destination must not exceed 512 characters");
        Preconditions.require(command.templateCode().length() <= 120,
                "Notification templateCode must not exceed 120 characters");
        Preconditions.require(command.idempotencyKey().length() <= 160,
                "Notification idempotencyKey must not exceed 160 characters");
        Map<String, String> parameters = command.parameters() == null ? Map.of() : command.parameters();
        Preconditions.require(parameters.size() <= 100,
                "Notification parameters must not contain more than 100 entries");
        parameters.forEach((key, value) -> {
            Preconditions.requireText(key, "notification parameter key");
            Preconditions.require(key.length() <= 200,
                    "Notification parameter keys must not exceed 200 characters");
            Preconditions.require(value == null || value.length() <= 8_192,
                    "Notification parameter values must not exceed 8192 characters");
        });
        if (command.deliverAfter() != null && command.expiresAt() != null) {
            Preconditions.require(command.deliverAfter().isBefore(command.expiresAt()),
                    "deliverAfter must be before expiresAt");
        }
    }
}
