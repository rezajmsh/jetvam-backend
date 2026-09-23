package ir.jetvam.modules.identity.persistence;

import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import ir.jetvam.modules.identity.model.OtpPurpose;
import ir.jetvam.modules.identity.model.OtpStatus;
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
 * Persists a hashed, rate-limited OTP challenge without retaining its plaintext code.
 * Optimistic locking prevents concurrent verification from consuming it twice.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Entity
@Table(name = "iam_otp_challenge")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OtpChallengeEntity extends AbstractAuditableUuidEntity {

    @Column(name = "mobile", nullable = false, length = 11)
    private String mobile;

    @Column(name = "national_code", length = 10)
    private String nationalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 40)
    private OtpPurpose purpose;

    @Column(name = "code_digest", nullable = false, length = 64)
    private String codeDigest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OtpStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "resend_available_at", nullable = false)
    private Instant resendAvailableAt;

    @Column(name = "attempts_remaining", nullable = false)
    private int attemptsRemaining;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    public OtpChallengeEntity(
            String mobile,
            String nationalCode,
            OtpPurpose purpose,
            String codeDigest,
            Instant expiresAt,
            Instant resendAvailableAt,
            int attemptsRemaining
    ) {
        this.mobile = mobile;
        this.nationalCode = nationalCode;
        this.purpose = purpose;
        this.codeDigest = codeDigest;
        this.expiresAt = expiresAt;
        this.resendAvailableAt = resendAvailableAt;
        this.attemptsRemaining = attemptsRemaining;
        this.status = OtpStatus.ACTIVE;
    }

    public void consume(Instant consumedAt) {
        this.status = OtpStatus.CONSUMED;
        this.consumedAt = consumedAt;
    }

    public void recordFailedAttempt() {
        this.attemptsRemaining--;
        if (attemptsRemaining <= 0) {
            this.status = OtpStatus.ATTEMPTS_EXHAUSTED;
        }
    }

    public void expire() {
        this.status = OtpStatus.EXPIRED;
    }
}
