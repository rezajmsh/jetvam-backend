package ir.jetvam.infra.observability.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.event.KeyValuePair;
import org.springframework.mock.env.MockEnvironment;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the behavior of otel structured log formatter.
 * The tests protect the shared contract and its important edge cases.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

class OtelStructuredLogFormatterTest {

    @Test
    void formatsTraceAndSemanticAttributesAsOtelAlignedJson() {
        LoggerContext context = new LoggerContext();
        context.putProperty("service.name", "jetvam-test");
        context.putProperty("service.version", "1.0.0");

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContextRemoteView(context.getLoggerContextRemoteView());
        event.setInstant(Instant.parse("2026-09-21T10:00:00Z"));
        event.setLevel(Level.INFO);
        event.setLoggerName("jetvam.http.server");
        event.setThreadName("test-thread");
        event.setMessage("HTTP request completed");
        event.setMDCPropertyMap(Map.of("traceId", "abc123", "spanId", "def456"));
        event.setKeyValuePairs(List.of(
                new KeyValuePair("event.type", "http.server.request"),
                new KeyValuePair("duration_ms", 12.5)
        ));

        String json = new OtelStructuredLogFormatter(new MockEnvironment()).format(event);

        assertThat(json)
                .startsWith("{\"timestamp\":")
                .contains("\"severity_number\":9")
                .contains("\"trace_id\":\"abc123\"")
                .contains("\"span_id\":\"def456\"")
                .contains("\"service.name\":\"jetvam-test\"")
                .contains("\"event.type\":\"http.server.request\"")
                .contains("\"duration_ms\":12.5");
    }
}
