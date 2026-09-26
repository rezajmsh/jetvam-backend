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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Measures and traces Spring Data repository method executions.
 * Slow calls and failures are logged with configurable thresholds.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@Aspect
@RequiredArgsConstructor
public class RepositoryObservabilityAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger("jetvam.repository");

    private final Tracer tracer;
    private final InfrastructureMetrics metrics;
    private final OtelEventLogger eventLogger;
    private final JetvamObservabilityProperties properties;

    @Around("execution(public * org.springframework.data.repository.Repository+.*(..))")
    public Object observe(ProceedingJoinPoint joinPoint) throws Throwable {
        String repository = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String operation = joinPoint.getSignature().getName();
        var settings = properties.getRepository();
        long startedAt = System.nanoTime();
        String outcome = "success";
        Throwable failure = null;
        RepositoryTelemetryContext.Scope telemetryScope = RepositoryTelemetryContext.open();
        RepositoryTelemetryContext.Snapshot telemetry = RepositoryTelemetryContext.Snapshot.empty();

        Span span = settings.isTracingEnabled()
                ? tracer.nextSpan()
                        .name("repository." + repository + "." + operation)
                        .tag("repository.name", repository)
                        .tag("repository.operation", operation)
                        .tag("code.namespace", joinPoint.getSignature().getDeclaringTypeName())
                        .tag("code.function.name", operation)
                        .start()
                : null;

        try (Tracer.SpanInScope ignored = span == null ? null : tracer.withSpan(span)) {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            outcome = "error";
            failure = throwable;
            if (span != null) {
                span.error(throwable);
            }
            throw throwable;
        } finally {
            telemetry = telemetryScope.finish();
            if (span != null) {
                span.end();
            }
            record(
                    joinPoint.getSignature().getDeclaringTypeName(),
                    repository,
                    operation,
                    outcome,
                    failure,
                    telemetry,
                    Duration.ofNanos(System.nanoTime() - startedAt)
            );
        }
    }

    private void record(
            String namespace,
            String repository,
            String operation,
            String outcome,
            Throwable failure,
            RepositoryTelemetryContext.Snapshot telemetry,
            Duration duration
    ) {
        var settings = properties.getRepository();
        boolean slow = duration.compareTo(settings.getSlowThreshold()) >= 0;
        boolean shouldLog = failure != null || slow || settings.isLogSuccessfulOperations();
        if (shouldLog && settings.isLoggingEnabled() && properties.getLogging().isEnabled()) {
            Level level = "error".equals(outcome) || slow ? Level.WARN : properties.getLogging().getLevel();
            eventLogger.log(
                    LOGGER,
                    level,
                    "repository.operation.completed",
                    OtelEventType.REPOSITORY_OPERATION,
                    "Repository operation completed",
                    eventLogger.attributes(
                            "event.phase", "end",
                            "span.kind", "internal",
                            "code.namespace", namespace,
                            "code.function.name", operation,
                            "repository.name", repository,
                            "repository.operation", operation,
                            "operation.outcome", outcome,
                            "operation.slow", slow,
                            "db.connection.acquire.count", telemetry.connectionAcquisitionCount(),
                            "db.connection.acquire.duration_ms", millis(telemetry.connectionAcquisitionDuration()),
                            "db.connection.reused",
                            telemetry.queryCount() > 0 && telemetry.connectionAcquisitionCount() == 0,
                            "db.statement.prepare.duration_ms", millis(telemetry.statementPreparationDuration()),
                            "db.statement.execute.duration_ms", millis(telemetry.statementExecutionDuration()),
                            "db.query.count", telemetry.queryCount(),
                            "db.query.captured_count", telemetry.queries().size(),
                            "db.query.truncated", telemetry.queriesTruncated(),
                            "db.queries", queries(telemetry),
                            "duration_ms", duration.toNanos() / 1_000_000.0
                    ),
                    failure
            );
        }
        if (settings.isMetricsEnabled() && properties.getMetrics().isEnabled()) {
            metrics.record("repository.operation.duration", duration, List.of(
                    Tag.of("repository", repository),
                    Tag.of("operation", operation),
                    Tag.of("outcome", outcome)
            ));
        }
    }

    private static List<Map<String, Object>> queries(RepositoryTelemetryContext.Snapshot telemetry) {
        return telemetry.queries().stream().map(query -> {
            Map<String, Object> attributes = new LinkedHashMap<>();
            attributes.put("text", query.text());
            attributes.put("text_truncated", query.textTruncated());
            attributes.put("prepare_duration_ms", millis(query.preparationDuration()));
            attributes.put("execute_duration_ms", millis(query.executionDuration()));
            return attributes;
        }).toList();
    }

    private static double millis(Duration duration) {
        return duration.toNanos() / 1_000_000.0;
    }
}
