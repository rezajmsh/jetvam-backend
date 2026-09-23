package ir.jetvam.modules.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Binds security controls for OTP length, expiry, retries and issuance rate limits.
 * The HMAC secret must be supplied externally in every non-test deployment.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Getter
@Setter
@ConfigurationProperties("jetvam.identity.security")
public class IdentitySecurityProperties {

    private final Otp otp = new Otp();

    /**
     * Defines one-time password generation, storage and abuse-prevention limits.
     * Defaults are conservative and can be overridden per deployment.
     *
     * @author reza jamshidi
     * @since 9/22/2026
     */
    @Getter
    @Setter
    public static class Otp {
        private int codeLength = 6;
        private Duration timeToLive = Duration.ofMinutes(2);
        private Duration resendInterval = Duration.ofSeconds(60);
        private Duration rateLimitWindow = Duration.ofHours(1);
        private int maxAttempts = 5;
        private int maxChallengesPerWindow = 5;
        private String hmacSecret;
    }
}
