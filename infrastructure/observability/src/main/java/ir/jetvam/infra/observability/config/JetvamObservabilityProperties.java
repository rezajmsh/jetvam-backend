package ir.jetvam.infra.observability.config;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.event.Level;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Binds external settings for the jetvam observability infrastructure.
 * Typed defaults and validation keep application configuration consistent.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@Getter
@Setter
@ConfigurationProperties(prefix = "jetvam.observability")
public class JetvamObservabilityProperties {

    private boolean enabled = true;
    private final Logging logging = new Logging();
    private final Tracing tracing = new Tracing();
    private final Metrics metrics = new Metrics();
    private final Http http = new Http();
    private final Repository repository = new Repository();
    private final Jdbc jdbc = new Jdbc();
    private final Audit audit = new Audit();

    /**
     * Configures structured event logging and exception stack traces.
     * Logging can be tuned independently from tracing and metrics.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Getter
    @Setter
    public static class Logging {
        private boolean enabled = true;
        private Level level = Level.INFO;
        private boolean includeStackTrace = true;
    }

    /**
     * Controls creation of application and infrastructure trace spans.
     * Export settings remain managed by standard Spring configuration.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Getter
    @Setter
    public static class Tracing {
        private boolean enabled = true;
    }

    /**
     * Controls publication of custom infrastructure metrics.
     * Standard JVM and process metrics remain provided by Actuator.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Getter
    @Setter
    public static class Metrics {
        private boolean enabled = true;
    }

    /**
     * Groups inbound and outbound HTTP observability settings.
     * Server and client instrumentation can be configured separately.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Getter
    public static class Http {
        private final Server server = new Server();
        private final Client client = new Client();
    }

    /**
     * Configures inbound HTTP request logs, metrics and safe header capture.
     * Excluded paths avoid noise from health and monitoring probes.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Getter
    @Setter
    public static class Server {
        private boolean enabled = true;
        private boolean logRequest = true;
        private boolean logResponse = true;
        private boolean metricsEnabled = true;
        private boolean includeQueryString = false;
        private String requestIdHeader = "X-Request-Id";
        private String clientIpHeader;
        private List<String> requestHeaderAllowList = new ArrayList<>(List.of(
                "User-Agent",
                "X-Request-Id",
                "X-Correlation-Id"
        ));
        private List<String> excludedPaths = new ArrayList<>(List.of(
                "/actuator/health",
                "/actuator/prometheus"
        ));
    }

    /**
     * Configures outbound HTTP client logging and metrics.
     * Instrumentation is applied automatically to managed client builders.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Getter
    @Setter
    public static class Client {
        private boolean enabled = true;
        private boolean loggingEnabled = true;
        private boolean metricsEnabled = true;
    }

    /**
     * Configures repository timing, spans and slow-operation logging.
     * Each signal can be enabled without forcing the others.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Getter
    @Setter
    public static class Repository {
        private boolean enabled = true;
        private boolean loggingEnabled = true;
        private boolean logSuccessfulOperations = true;
        private boolean metricsEnabled = true;
        private boolean tracingEnabled = true;
        private Duration slowThreshold = Duration.ofMillis(500);
    }

    /**
     * Configures tracing and timing for unavoidable direct JDBC framework stores.
     * SQL text and bind values are deliberately excluded from logs and span tags.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Getter
    @Setter
    public static class Jdbc {
        private boolean enabled = true;
        private boolean loggingEnabled = true;
        private boolean logSuccessfulOperations = false;
        private boolean metricsEnabled = true;
        private boolean tracingEnabled = true;
        private Duration slowThreshold = Duration.ofMillis(500);
    }

    /**
     * Configures structured audit events and outcome metrics.
     * Audit logging uses a dedicated category and severity.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Getter
    @Setter
    public static class Audit {
        private boolean enabled = true;
        private boolean metricsEnabled = true;
        private Level level = Level.INFO;
    }
}
