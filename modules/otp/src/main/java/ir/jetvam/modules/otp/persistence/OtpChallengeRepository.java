package ir.jetvam.modules.otp.persistence;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.otp.OtpStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides persistence access for reusable OTP issuance and verification.
 * Derived queries keep consumers independent from the persistence implementation.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public interface OtpChallengeRepository extends JetvamJpaRepository<OtpChallengeEntity, UUID> {

    Optional<OtpChallengeEntity> findFirstByMobileAndPurposeAndStatusOrderByCreatedAtDesc(
            String mobile,
            String purpose,
            OtpStatus status
    );

    long countByMobileAndPurposeAndCreatedAtAfter(String mobile, String purpose, Instant createdAfter);
}
