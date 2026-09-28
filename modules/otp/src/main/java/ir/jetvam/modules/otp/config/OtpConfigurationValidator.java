package ir.jetvam.modules.otp.config;

import ir.jetvam.common.validation.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

/**
 * Validates OTP security and lifecycle settings before the application accepts traffic.
 * Development fixed codes are rejected unless the declared environment is explicitly allowed.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OtpConfigurationValidator implements InitializingBean {

    private static final Set<String> DEVELOPMENT_ENVIRONMENTS = Set.of("local", "development", "test");

    private final OtpProperties properties;

    @Override
    public void afterPropertiesSet() {
        Preconditions.requireInRange(properties.getCodeLength(), 4, 8, "jetvam.otp.code-length");
        requirePositive(properties.getTimeToLive(), "jetvam.otp.time-to-live");
        requirePositive(properties.getResendInterval(), "jetvam.otp.resend-interval");
        requirePositive(properties.getRateLimitWindow(), "jetvam.otp.rate-limit-window");
        Preconditions.requirePositive(properties.getMaxAttempts(), "jetvam.otp.max-attempts");
        Preconditions.requirePositive(
                properties.getMaxChallengesPerWindow(),
                "jetvam.otp.max-challenges-per-window"
        );
        String secret = Preconditions.requireText(properties.getHmacSecret(), "jetvam.otp.hmac-secret");
        Preconditions.require(secret.length() >= 32, "OTP HMAC secret must contain at least 32 characters");
        validateDevelopmentBypass();
    }

    private void validateDevelopmentBypass() {
        OtpProperties.DevelopmentBypass bypass = properties.getDevelopmentBypass();
        if (!bypass.isEnabled()) {
            return;
        }

        String environment = Preconditions.requireText(
                bypass.getEnvironment(),
                "jetvam.otp.development-bypass.environment"
        );
        Preconditions.require(
                DEVELOPMENT_ENVIRONMENTS.stream().anyMatch(environment::equalsIgnoreCase),
                "OTP development bypass cannot be enabled in environment " + environment
        );

        String code = Preconditions.requireText(bypass.getCode(), "jetvam.otp.development-bypass.code");
        Preconditions.require(
                code.matches("\\d{" + properties.getCodeLength() + "}"),
                "OTP development bypass code must contain exactly " + properties.getCodeLength() + " digits"
        );
        log.warn("OTP development bypass is enabled for environment {}; never enable it in production", environment);
    }

    private static void requirePositive(Duration duration, String propertyName) {
        Preconditions.requireNonNull(duration, propertyName);
        Preconditions.require(!duration.isZero() && !duration.isNegative(), propertyName + " must be positive");
    }
}
