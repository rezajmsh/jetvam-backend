package ir.jetvam.modules.notification.persistence;

import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import ir.jetvam.modules.notification.model.NotificationChannel;
import ir.jetvam.modules.notification.model.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Persists encrypted notification payloads and their retry-safe delivery state.
 * A lease allows crashed workers to be recovered without losing messages.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Entity
@Table(name = "notification_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationOutboxEntity extends AbstractAuditableUuidEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(name = "template_code", nullable = false, length = 120)
    private String templateCode;

    @Column(name = "encrypted_payload", nullable = false, columnDefinition = "text")
    private String encryptedPayload;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 160)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "provider_message_id", length = 200)
    private String providerMessageId;

    @Column(name = "last_error", length = 120)
    private String lastError;

    public NotificationOutboxEntity(
            NotificationChannel channel,
            String templateCode,
            String encryptedPayload,
            String idempotencyKey,
            int maxAttempts,
            Instant nextAttemptAt,
            Instant expiresAt
    ) {
        this.channel = channel;
        this.templateCode = templateCode;
        this.encryptedPayload = encryptedPayload;
        this.idempotencyKey = idempotencyKey;
        this.maxAttempts = maxAttempts;
        this.nextAttemptAt = nextAttemptAt;
        this.expiresAt = expiresAt;
        this.status = NotificationStatus.PENDING;
    }

    public boolean canBeClaimed(Instant now) {
        return status != NotificationStatus.SENT
                && status != NotificationStatus.DEAD
                && !nextAttemptAt.isAfter(now);
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    public boolean canBeCancelled() {
        return status != NotificationStatus.SENT && status != NotificationStatus.DEAD;
    }

    public void claim(Instant leaseUntil) {
        status = NotificationStatus.PROCESSING;
        attemptCount++;
        nextAttemptAt = leaseUntil;
        lastError = null;
    }

    public void markSent(Instant now, String messageId) {
        status = NotificationStatus.SENT;
        sentAt = now;
        providerMessageId = truncate(messageId, 200);
        lastError = null;
    }

    public void markFailed(Instant retryAt, String errorCode) {
        lastError = truncate(errorCode, 120);
        if (attemptCount >= maxAttempts) {
            status = NotificationStatus.DEAD;
        } else {
            status = NotificationStatus.RETRY;
            nextAttemptAt = retryAt;
        }
    }

    public void markDead(String errorCode) {
        status = NotificationStatus.DEAD;
        lastError = truncate(errorCode, 120);
    }

    private static String truncate(String value, int maximumLength) {
        if (value == null || value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, maximumLength);
    }
}
