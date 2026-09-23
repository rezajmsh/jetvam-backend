package ir.jetvam.infra.observability.audit;

import io.micrometer.core.instrument.Tag;
import ir.jetvam.infra.observability.config.JetvamObservabilityProperties;
import ir.jetvam.infra.observability.logging.OtelEventLogger;
import ir.jetvam.infra.observability.logging.OtelEventType;
import ir.jetvam.infra.observability.metrics.InfrastructureMetrics;
import ir.jetvam.infra.observability.trace.TraceContextProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Publishes audit events as structured OpenTelemetry-compatible logs.
 * It also records configurable audit outcome metrics.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@RequiredArgsConstructor
public class DefaultAuditLogger implements AuditLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger("jetvam.audit");

    private final OtelEventLogger eventLogger;
    private final InfrastructureMetrics metrics;
    private final TraceContextProvider traceContextProvider;
    private final JetvamObservabilityProperties properties;

    @Override
    public void record(AuditEvent event) {
        if (!properties.getAudit().isEnabled()) {
            return;
        }

        var attributes = new LinkedHashMap<String, Object>(event.getAttributes());
        putIfPresent(attributes, "audit.actor.type", event.getActorType());
        putIfPresent(attributes, "audit.actor.id", event.getActorId());
        putIfPresent(attributes, "audit.subject.type", event.getSubjectType());
        putIfPresent(attributes, "audit.subject.id", event.getSubjectId());
        attributes.put("audit.action", event.getAction());
        attributes.put("audit.outcome", event.getOutcome());

        var traceContext = traceContextProvider.current();
        if (traceContext.present()) {
            attributes.put("trace_id", traceContext.traceId());
            attributes.put("span_id", traceContext.spanId());
        }

        eventLogger.log(
                LOGGER,
                properties.getAudit().getLevel(),
                "audit." + event.getAction(),
                OtelEventType.AUDIT,
                "Audit event recorded",
                attributes
        );

        if (properties.getMetrics().isEnabled() && properties.getAudit().isMetricsEnabled()) {
            metrics.increment("audit.events", List.of(
                    Tag.of("action", event.getAction()),
                    Tag.of("outcome", event.getOutcome())
            ));
        }
    }

    private static void putIfPresent(LinkedHashMap<String, Object> attributes, String key, String value) {
        if (value != null && !value.isBlank()) {
            attributes.put(key, value);
        }
    }
}
