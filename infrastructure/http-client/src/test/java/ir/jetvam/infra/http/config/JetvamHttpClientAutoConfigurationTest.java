package ir.jetvam.infra.http.config;

import ir.jetvam.infra.http.JetvamHttpClientFactory;
import ir.jetvam.infra.observability.config.JetvamObservabilityAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies classpath-driven creation of the shared HTTP client facility.
 * Applications can override the managed RestClient builder without duplicate beans.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
class JetvamHttpClientAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JetvamObservabilityAutoConfiguration.class,
                    JetvamHttpClientAutoConfiguration.class
            ));

    @Test
    void createsFactoryAndDefaultBuilder() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(JetvamHttpClientFactory.class);
            assertThat(context).hasSingleBean(RestClient.Builder.class);
            assertThat(context).hasSingleBean(JetvamHttpClientProperties.class);
        });
    }
}
