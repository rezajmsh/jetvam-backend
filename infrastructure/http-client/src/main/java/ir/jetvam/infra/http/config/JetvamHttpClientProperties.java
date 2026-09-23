package ir.jetvam.infra.http.config;

import ir.jetvam.common.validation.Preconditions;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Binds default and named outbound HTTP client transport settings.
 * Named values override defaults without leaking provider concerns into applications.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Getter
@Setter
@ConfigurationProperties("jetvam.http-client")
public class JetvamHttpClientProperties {

    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(5);
    private boolean observabilityEnabled = true;
    private final Map<String, Client> clients = new LinkedHashMap<>();

    public Client resolve(String name) {
        Client configured = clients.get(name);
        if (configured == null) {
            return validated(new Client(connectTimeout, readTimeout, observabilityEnabled), name);
        }
        return validated(new Client(
                configured.connectTimeout == null ? connectTimeout : configured.connectTimeout,
                configured.readTimeout == null ? readTimeout : configured.readTimeout,
                configured.observabilityEnabled == null ? observabilityEnabled : configured.observabilityEnabled
        ), name);
    }

    private static Client validated(Client client, String name) {
        Preconditions.requireNonNull(client.connectTimeout, "jetvam.http-client.clients." + name + ".connect-timeout");
        Preconditions.requireNonNull(client.readTimeout, "jetvam.http-client.clients." + name + ".read-timeout");
        Preconditions.require(!client.connectTimeout.isZero() && !client.connectTimeout.isNegative(),
                "HTTP connect timeout must be positive");
        Preconditions.require(!client.readTimeout.isZero() && !client.readTimeout.isNegative(),
                "HTTP read timeout must be positive");
        return client;
    }

    /**
     * Holds optional timeout overrides for a named external dependency.
     * Missing values inherit the facility-wide defaults.
     *
     * @author reza jamshidi
     * @since 9/22/2026
     */
    @Getter
    @Setter
    public static class Client {
        private Duration connectTimeout;
        private Duration readTimeout;
        private Boolean observabilityEnabled;

        public Client() {
        }

        private Client(Duration connectTimeout, Duration readTimeout, boolean observabilityEnabled) {
            this.connectTimeout = connectTimeout;
            this.readTimeout = readTimeout;
            this.observabilityEnabled = observabilityEnabled;
        }

        public boolean isObservabilityEnabled() {
            return Boolean.TRUE.equals(observabilityEnabled);
        }
    }
}
