package ir.jetvam.infra.http;

import io.micrometer.observation.ObservationRegistry;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.http.config.JetvamHttpClientProperties;
import ir.jetvam.infra.http.observability.HttpClientObservabilityInterceptor;
import ir.jetvam.infra.observability.config.JetvamObservabilityProperties;
import ir.jetvam.infra.observability.logging.OtelEventLogger;
import ir.jetvam.infra.observability.metrics.InfrastructureMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;

/**
 * Creates isolated, named RestClient instances with central timeouts and observability.
 * Cloning the managed builder preserves trace propagation and application customizers.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@RequiredArgsConstructor
public class JetvamHttpClientFactory {

    private final RestClient.Builder builder;
    private final JetvamHttpClientProperties clientProperties;
    private final JetvamObservabilityProperties observabilityProperties;
    private final ObservationRegistry observationRegistry;
    private final OtelEventLogger eventLogger;
    private final InfrastructureMetrics metrics;

    public RestClient create(String clientName, String baseUrl) {
        Preconditions.requireText(clientName, "clientName");
        Preconditions.requireText(baseUrl, "baseUrl");
        return create(clientName, URI.create(baseUrl));
    }

    public RestClient create(String clientName, URI baseUrl) {
        Preconditions.requireText(clientName, "clientName");
        Preconditions.requireNonNull(baseUrl, "baseUrl");
        JetvamHttpClientProperties.Client settings = clientProperties.resolve(clientName);
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(settings.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(settings.getReadTimeout());

        boolean observable = observabilityProperties.isEnabled() && settings.isObservabilityEnabled();
        RestClient.Builder configured = builder.clone()
                .baseUrl(baseUrl.toString())
                .requestFactory(requestFactory)
                .observationRegistry(observable ? observationRegistry : ObservationRegistry.NOOP);
        if (observable && observabilityProperties.getHttp().getClient().isEnabled()) {
            configured.requestInterceptor(new HttpClientObservabilityInterceptor(
                    clientName,
                    observabilityProperties,
                    eventLogger,
                    metrics
            ));
        }
        return configured.build();
    }
}
