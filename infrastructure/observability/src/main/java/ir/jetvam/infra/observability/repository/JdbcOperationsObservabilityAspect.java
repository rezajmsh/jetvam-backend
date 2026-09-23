package ir.jetvam.infra.observability.repository;

import io.micrometer.core.instrument.Tag;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import ir.jetvam.infra.observability.config.JetvamObservabilityProperties;
import ir.jetvam.infra.observability.logging.OtelEventLogger;
import ir.jetvam.infra.observability.logging.OtelEventType;
import ir.jetvam.infra.observability.metrics.InfrastructureMetrics;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.time.Duration;
import java.util.List;

/**
 * Observes direct Spring JDBC operations used by framework persistence stores.
 * It emits duration, outcome, trace and metrics without exposing SQL parameters.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Aspect
@RequiredArgsConstructor
public class JdbcOperationsObservabilityAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger("jetvam.jdbc");

    private final Tracer tracer;
    private final InfrastructureMetrics metrics;
    private final OtelEventLogger eventLogger;
    private final JetvamObservabilityProperties properties;

    @Around("execution(public * org.springframework.jdbc.core.JdbcOperations+.*(..))")
    public Object observe(ProceedingJoinPoint joinPoint) throws Throwable {
        String component = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String operation = joinPoint.getSignature().getName();
        var settings = properties.getJdbc();
        long startedAt = System.nanoTime();
        String outcome = "success";
        Span span = settings.isTracingEnabled()
                ? tracer.nextSpan()
                        .name("jdbc.operation")
                        .tag("db.operation.name", operation)
                        .tag("code.namespace", component)
                        .start()
                : null;
        try (Tracer.SpanInScope ignored = span == null ? null : tracer.withSpan(span)) {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            outcome = "error";
            if (span != null) {
                span.error(throwable);
            }
            throw throwable;
        } finally {
            if (span != null) {
                span.end();
            }
            record(component, operation, outcome, Duration.ofNanos(System.nanoTime() - startedAt));
        }
    }

    private void record(String component, String operation, String outcome, Duration duration) {
        var settings = properties.getJdbc();
        boolean slow = duration.compareTo(settings.getSlowThreshold()) >= 0;
        if (settings.isLoggingEnabled() && properties.getLogging().isEnabled()) {
            Level level = "error".equals(outcome) || slow ? Level.WARN : properties.getLogging().getLevel();
            eventLogger.log(
                    LOGGER,
                    level,
                    "jdbc.operation.completed",
                    OtelEventType.REPOSITORY_OPERATION,
                    "JDBC operation completed",
                    eventLogger.attributes(
                            "code.namespace", component,
                            "db.operation.name", operation,
                            "operation.outcome", outcome,
                            "operation.slow", slow,
                            "duration_ms", duration.toNanos() / 1_000_000.0
                    )
            );
        }
        if (settings.isMetricsEnabled() && properties.getMetrics().isEnabled()) {
            metrics.record("jdbc.operation.duration", duration, List.of(
                    Tag.of("component", component),
                    Tag.of("operation", operation),
                    Tag.of("outcome", outcome)
            ));
        }
    }
}
