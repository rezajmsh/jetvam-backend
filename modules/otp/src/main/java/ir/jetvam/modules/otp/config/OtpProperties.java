package ir.jetvam.modules.otp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Binds reusable OTP generation, expiry, retry and issuance-rate controls.
 * The HMAC secret must be supplied externally in every non-test deployment.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Getter
@Setter
@ConfigurationProperties("jetvam.otp")
public class OtpProperties {

    private int codeLength = 6;
    private Duration timeToLive = Duration.ofMinutes(2);
    private Duration resendInterval = Duration.ofSeconds(60);
    private Duration rateLimitWindow = Duration.ofHours(1);
    private int maxAttempts = 5;
    private int maxChallengesPerWindow = 5;
    private String hmacSecret;
    private final DevelopmentBypass developmentBypass = new DevelopmentBypass();

    /**
     * Configures a deterministic OTP code for environments without an SMS provider.
     * The owning validator prevents this facility from being enabled outside explicitly allowed environments.
     *
     * @author reza jamshidi
     * @since 9/28/2026
     */
    @Getter
    @Setter
    public static class DevelopmentBypass {

        private boolean enabled;
        private String code = "111111";
        private String environment = "local";
    }
}
