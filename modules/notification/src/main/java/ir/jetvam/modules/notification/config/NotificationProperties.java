package ir.jetvam.modules.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Binds queue security, retry scheduling and notification observability settings.
 * External delivery providers are owned by the independent integration module.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Getter
@ConfigurationProperties("jetvam.notification")
public class NotificationProperties {

    private final Outbox outbox = new Outbox();
    private final Dispatcher dispatcher = new Dispatcher();
    private final Observability observability = new Observability();

    /**
     * Configures encryption and retry policy for durable notification records.
     * Payload encryption prevents OTPs and destinations from being stored in plaintext.
     *
     * @author reza jamshidi
     * @since 9/22/2026
     */
    @Getter
    @Setter
    public static class Outbox {
        private String encryptionKeyBase64;
        private int maxAttempts = 5;
        private Duration initialRetryDelay = Duration.ofSeconds(2);
        private Duration maximumRetryDelay = Duration.ofMinutes(1);
        private Duration leaseDuration = Duration.ofSeconds(30);
    }

    /**
     * Controls the local outbox worker and the amount of work per poll.
     * Each producer application can independently enable its transactional worker.
     *
     * @author reza jamshidi
     * @since 9/22/2026
     */
    @Getter
    @Setter
    public static class Dispatcher {
        private boolean enabled;
        private Duration fixedDelay = Duration.ofSeconds(1);
        private int batchSize = 25;
    }

    /**
     * Controls safe lifecycle logs and bounded metrics for notification delivery.
     * It complements the global observability switches with module-level overrides.
     *
     * @author reza jamshidi
     * @since 9/22/2026
     */
    @Getter
    @Setter
    public static class Observability {
        private boolean enabled = true;
        private boolean loggingEnabled = true;
        private boolean metricsEnabled = true;
    }
}
