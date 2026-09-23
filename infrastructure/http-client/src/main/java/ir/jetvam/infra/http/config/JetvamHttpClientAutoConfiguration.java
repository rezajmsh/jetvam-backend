package ir.jetvam.infra.http.config;

import io.micrometer.observation.ObservationRegistry;
import ir.jetvam.infra.http.JetvamHttpClientFactory;
import ir.jetvam.infra.observability.config.JetvamObservabilityAutoConfiguration;
import ir.jetvam.infra.observability.config.JetvamObservabilityProperties;
import ir.jetvam.infra.observability.logging.OtelEventLogger;
import ir.jetvam.infra.observability.metrics.InfrastructureMetrics;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Auto-configures the shared named HTTP client factory in web and worker applications.
 * Applications only provide endpoint URLs and optional per-client timeout overrides.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@AutoConfiguration(after = JetvamObservabilityAutoConfiguration.class)
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties({JetvamHttpClientProperties.class, JetvamObservabilityProperties.class})
public class JetvamHttpClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RestClient.Builder jetvamRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @ConditionalOnMissingBean
    JetvamHttpClientFactory jetvamHttpClientFactory(
            RestClient.Builder builder,
            JetvamHttpClientProperties clientProperties,
            JetvamObservabilityProperties observabilityProperties,
            ObjectProvider<ObservationRegistry> observationRegistry,
            OtelEventLogger eventLogger,
            InfrastructureMetrics metrics
    ) {
        return new JetvamHttpClientFactory(
                builder,
                clientProperties,
                observabilityProperties,
                observationRegistry.getIfAvailable(() -> ObservationRegistry.NOOP),
                eventLogger,
                metrics
        );
    }
}
