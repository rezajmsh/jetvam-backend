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

        Span span = settings.isTracingEnabled()
                ? tracer.nextSpan()
                        .name("repository.operation")
                        .tag("repository.name", repository)
                        .tag("repository.operation", operation)
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
            record(repository, operation, outcome, Duration.ofNanos(System.nanoTime() - startedAt));
        }
    }

    private void record(String repository, String operation, String outcome, Duration duration) {
        var settings = properties.getRepository();
        boolean slow = duration.compareTo(settings.getSlowThreshold()) >= 0;
        if (settings.isLoggingEnabled() && properties.getLogging().isEnabled()) {
            Level level = "error".equals(outcome) || slow ? Level.WARN : properties.getLogging().getLevel();
            eventLogger.log(
                    LOGGER,
                    level,
                    "repository.operation.completed",
                    OtelEventType.REPOSITORY_OPERATION,
                    "Repository operation completed",
                    eventLogger.attributes(
                            "repository.name", repository,
                            "repository.operation", operation,
                            "operation.outcome", outcome,
                            "operation.slow", slow,
                            "duration_ms", duration.toNanos() / 1_000_000.0
                    )
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
}
