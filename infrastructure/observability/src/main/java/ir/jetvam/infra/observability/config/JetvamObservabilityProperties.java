package ir.jetvam.infra.observability.config;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.event.Level;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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
    private final Audit audit = new Audit();

    @Getter
    @Setter
    public static class Logging {
        private boolean enabled = true;
        private Level level = Level.INFO;
        private boolean includeStackTrace = true;
    }

    @Getter
    @Setter
    public static class Tracing {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Metrics {
        private boolean enabled = true;
    }

    @Getter
    public static class Http {
        private final Server server = new Server();
        private final Client client = new Client();
    }

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

    @Getter
    @Setter
    public static class Client {
        private boolean enabled = true;
        private boolean loggingEnabled = true;
        private boolean metricsEnabled = true;
    }

    @Getter
    @Setter
    public static class Repository {
        private boolean enabled = true;
        private boolean loggingEnabled = true;
        private boolean metricsEnabled = true;
        private boolean tracingEnabled = true;
        private Duration slowThreshold = Duration.ofMillis(500);
    }

    @Getter
    @Setter
    public static class Audit {
        private boolean enabled = true;
        private boolean metricsEnabled = true;
        private Level level = Level.INFO;
    }
}
