package ir.jetvam.infra.http.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies named timeout inheritance and fail-fast transport validation.
 * Invalid durations must be rejected before an external call is attempted.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
class JetvamHttpClientPropertiesTest {

    @Test
    void mergesNamedOverridesWithDefaults() {
        JetvamHttpClientProperties properties = new JetvamHttpClientProperties();
        properties.setConnectTimeout(Duration.ofSeconds(2));
        properties.setReadTimeout(Duration.ofSeconds(7));
        JetvamHttpClientProperties.Client sms = new JetvamHttpClientProperties.Client();
        sms.setReadTimeout(Duration.ofSeconds(10));
        sms.setObservabilityEnabled(false);
        properties.getClients().put("notification-sms", sms);

        JetvamHttpClientProperties.Client resolved = properties.resolve("notification-sms");

        assertThat(resolved.getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(resolved.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(resolved.isObservabilityEnabled()).isFalse();
    }

    @Test
    void rejectsNonPositiveTimeouts() {
        JetvamHttpClientProperties properties = new JetvamHttpClientProperties();
        properties.setConnectTimeout(Duration.ZERO);

        assertThatThrownBy(() -> properties.resolve("shahkar"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("HTTP connect timeout must be positive");
    }
}
