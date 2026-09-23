package ir.jetvam.modules.identity.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.identity.model.OtpPurpose;
import ir.jetvam.modules.identity.model.OtpStatus;
import ir.jetvam.modules.identity.persistence.OtpChallengeEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides centralized persistence access for OTP issuance and verification.
 * Derived queries keep identity code independent of Spring Data implementation APIs.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public interface OtpChallengeRepository extends JetvamJpaRepository<OtpChallengeEntity, UUID> {

    Optional<OtpChallengeEntity> findFirstByMobileAndPurposeAndStatusOrderByCreatedAtDesc(
            String mobile,
            OtpPurpose purpose,
            OtpStatus status
    );

    long countByMobileAndPurposeAndCreatedAtAfter(String mobile, OtpPurpose purpose, Instant createdAfter);
}
