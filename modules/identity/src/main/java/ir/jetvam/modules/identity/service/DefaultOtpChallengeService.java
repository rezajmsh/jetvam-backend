package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.common.exception.RateLimitExceededException;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.config.IdentitySecurityProperties;
import ir.jetvam.modules.identity.model.OtpPurpose;
import ir.jetvam.modules.identity.model.OtpStatus;
import ir.jetvam.modules.identity.persistence.OtpChallengeEntity;
import ir.jetvam.modules.identity.repository.OtpChallengeRepository;
import ir.jetvam.modules.notification.model.NotificationChannel;
import ir.jetvam.modules.notification.service.NotificationCommand;
import ir.jetvam.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

/**
 * Generates cryptographically random OTPs and stores only HMAC digests.
 * Database rate limits, retries, expiry and optimistic locking prevent common abuse.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultOtpChallengeService implements OtpChallengeService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final OtpChallengeRepository repository;
    private final NotificationService notificationService;
    private final IdentitySecurityProperties properties;
    private final TimeProvider timeProvider;

    @Override
    @Transactional
    public OtpChallengeView issue(String mobile, String nationalCode, OtpPurpose purpose) {
        Preconditions.requireText(mobile, "mobile");
        Preconditions.requireNonNull(purpose, "purpose");
        IdentitySecurityProperties.Otp settings = validatedSettings();
        Instant now = timeProvider.now();

        repository.findFirstByMobileAndPurposeAndStatusOrderByCreatedAtDesc(mobile, purpose, OtpStatus.ACTIVE)
                .ifPresent(challenge -> expireForResend(challenge, now));
        long issued = repository.countByMobileAndPurposeAndCreatedAtAfter(
                mobile,
                purpose,
                now.minus(settings.getRateLimitWindow())
        );
        if (issued >= settings.getMaxChallengesPerWindow()) {
            throw new RateLimitExceededException(
                    "Too many OTP requests; try again later",
                    now.plus(settings.getRateLimitWindow())
            );
        }

        String code = generateCode(settings.getCodeLength());
        OtpChallengeEntity challenge = repository.saveAndFlush(new OtpChallengeEntity(
                mobile,
                nationalCode,
                purpose,
                digest(mobile, purpose, code, settings.getHmacSecret()),
                now.plus(settings.getTimeToLive()),
                now.plus(settings.getResendInterval()),
                settings.getMaxAttempts()
        ));
        notificationService.enqueue(new NotificationCommand(
                NotificationChannel.SMS,
                mobile,
                templateCode(purpose),
                Map.of("code", code, "purpose", purpose.name()),
                notificationIdempotencyKey(challenge),
                now,
                challenge.getExpiresAt()
        ));
        return toView(challenge);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ValidationException.class)
    public OtpVerificationData consume(UUID challengeId, String code, OtpPurpose purpose) {
        Preconditions.requireNonNull(challengeId, "challengeId");
        Preconditions.requireText(code, "otp");
        Preconditions.requireNonNull(purpose, "purpose");
        OtpChallengeEntity challenge = repository.findByIdForUpdate(challengeId)
                .orElseThrow(DefaultOtpChallengeService::invalidOtp);
        Instant now = timeProvider.now();

        if (challenge.getPurpose() != purpose || challenge.getStatus() != OtpStatus.ACTIVE) {
            throw invalidOtp();
        }
        if (!now.isBefore(challenge.getExpiresAt())) {
            challenge.expire();
            repository.saveAndFlush(challenge);
            throw invalidOtp();
        }
        String actualDigest = digest(challenge.getMobile(), purpose, code, validatedSettings().getHmacSecret());
        if (!MessageDigest.isEqual(
                challenge.getCodeDigest().getBytes(StandardCharsets.US_ASCII),
                actualDigest.getBytes(StandardCharsets.US_ASCII)
        )) {
            challenge.recordFailedAttempt();
            repository.saveAndFlush(challenge);
            throw invalidOtp();
        }

        challenge.consume(now);
        repository.saveAndFlush(challenge);
        return new OtpVerificationData(challenge.getMobile(), challenge.getNationalCode());
    }

    private IdentitySecurityProperties.Otp validatedSettings() {
        IdentitySecurityProperties.Otp settings = properties.getOtp();
        Preconditions.require(settings.getCodeLength() >= 4 && settings.getCodeLength() <= 8,
                "OTP codeLength must be between 4 and 8");
        Preconditions.require(settings.getMaxAttempts() > 0, "OTP maxAttempts must be positive");
        Preconditions.require(settings.getMaxChallengesPerWindow() > 0,
                "OTP maxChallengesPerWindow must be positive");
        String secret = Preconditions.requireText(settings.getHmacSecret(), "jetvam.identity.security.otp.hmac-secret");
        Preconditions.require(secret.length() >= 32, "OTP HMAC secret must contain at least 32 characters");
        return settings;
    }

    private void expireForResend(OtpChallengeEntity challenge, Instant now) {
        if (!now.isBefore(challenge.getExpiresAt())) {
            challenge.expire();
            notificationService.cancel(notificationIdempotencyKey(challenge));
            return;
        }
        if (now.isBefore(challenge.getResendAvailableAt())) {
            throw new RateLimitExceededException(
                    "OTP resend is not available yet",
                    challenge.getResendAvailableAt()
            );
        }
        challenge.expire();
        notificationService.cancel(notificationIdempotencyKey(challenge));
    }

    private static String generateCode(int length) {
        int bound = (int) Math.pow(10, length);
        return String.format("%0" + length + "d", SECURE_RANDOM.nextInt(bound));
    }

    private static String digest(String mobile, OtpPurpose purpose, String code, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] bytes = mac.doFinal((purpose.name() + ':' + mobile + ':' + code)
                    .getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("OTP digest algorithm is unavailable", exception);
        }
    }

    private static OtpChallengeView toView(OtpChallengeEntity challenge) {
        return new OtpChallengeView(
                challenge.getId(),
                challenge.getExpiresAt(),
                challenge.getResendAvailableAt()
        );
    }

    private static ValidationException invalidOtp() {
        return new ValidationException("OTP is invalid or expired");
    }

    private static String templateCode(OtpPurpose purpose) {
        return switch (purpose) {
            case CUSTOMER_REGISTRATION -> "identity.otp.customer-registration";
            case CUSTOMER_LOGIN -> "identity.otp.customer-login";
        };
    }

    private static String notificationIdempotencyKey(OtpChallengeEntity challenge) {
        return "identity-otp:" + challenge.getId();
    }
}
