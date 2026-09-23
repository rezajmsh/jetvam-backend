package ir.jetvam.infra.http.observability;

import io.micrometer.core.instrument.Tag;
import ir.jetvam.infra.observability.config.JetvamObservabilityProperties;
import ir.jetvam.infra.observability.logging.OtelEventLogger;
import ir.jetvam.infra.observability.logging.OtelEventType;
import ir.jetvam.infra.observability.metrics.InfrastructureMetrics;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Records safe OTel-compatible logs and bounded metrics for one named HTTP client.
 * Request bodies, query values, credentials and response bodies are never captured.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@RequiredArgsConstructor
public class HttpClientObservabilityInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger("jetvam.http.client");

    private final String clientName;
    private final JetvamObservabilityProperties properties;
    private final OtelEventLogger eventLogger;
    private final InfrastructureMetrics metrics;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        long startedAt = System.nanoTime();
        int status = 0;
        Throwable failure = null;
        try {
            ClientHttpResponse response = execution.execute(request, body);
            status = response.getStatusCode().value();
            return response;
        } catch (IOException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            record(request, body.length, status, Duration.ofNanos(System.nanoTime() - startedAt), failure);
        }
    }

    private void record(HttpRequest request, int bodySize, int status, Duration duration, Throwable failure) {
        var settings = properties.getHttp().getClient();
        String outcome = failure != null || status >= 400 ? "error" : "success";
        String host = request.getURI().getHost() == null ? "UNKNOWN" : request.getURI().getHost();
        if (properties.getLogging().isEnabled() && settings.isLoggingEnabled()) {
            eventLogger.log(
                    LOGGER,
                    failure == null && status < 400 ? properties.getLogging().getLevel() : Level.ERROR,
                    "http.client.request.completed",
                    OtelEventType.HTTP_CLIENT_REQUEST,
                    "Outbound HTTP request completed",
                    eventLogger.attributes(
                            "http.client.name", clientName,
                            "http.request.method", request.getMethod().name(),
                            "url.scheme", request.getURI().getScheme(),
                            "server.address", host,
                            "server.port", effectivePort(request),
                            "url.path", request.getURI().getPath(),
                            "http.request.body.size", bodySize,
                            "http.response.status_code", status,
                            "operation.outcome", outcome,
                            "error.type", failure == null ? null : failure.getClass().getName(),
                            "duration_ms", duration.toNanos() / 1_000_000.0
                    )
            );
        }
        if (properties.getMetrics().isEnabled() && settings.isMetricsEnabled()) {
            metrics.record("http.client.request.duration", duration, List.of(
                    Tag.of("client", clientName),
                    Tag.of("method", request.getMethod().name()),
                    Tag.of("server", host),
                    Tag.of("status", status == 0 ? "IO_ERROR" : String.valueOf(status)),
                    Tag.of("outcome", outcome)
            ));
        }
    }

    private static int effectivePort(HttpRequest request) {
        int explicitPort = request.getURI().getPort();
        if (explicitPort >= 0) {
            return explicitPort;
        }
        return "https".equalsIgnoreCase(request.getURI().getScheme()) ? 443 : 80;
    }
}
