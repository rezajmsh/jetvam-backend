package ir.jetvam.infra.observability.repository;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.tracing.Tracer;
import ir.jetvam.infra.observability.config.JetvamObservabilityProperties;
import ir.jetvam.infra.observability.logging.OtelEventLogger;
import ir.jetvam.infra.observability.metrics.InfrastructureMetrics;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.core.JdbcOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies metrics emitted for framework JDBC persistence operations.
 * SQL text and bind values intentionally remain outside observable attributes.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
class JdbcOperationsObservabilityAspectTest {

    @Test
    void recordsComponentOperationAndOutcome() throws Throwable {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("meterRegistry", registry);
        var properties = new JetvamObservabilityProperties();
        properties.getLogging().setEnabled(false);
        var aspect = new JdbcOperationsObservabilityAspect(
                Tracer.NOOP,
                new InfrastructureMetrics(beanFactory.getBeanProvider(MeterRegistry.class)),
                new OtelEventLogger(),
                properties
        );
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn((Class) JdbcOperations.class);
        when(signature.getName()).thenReturn("update");
        when(joinPoint.proceed()).thenReturn(1);

        Object result = aspect.observe(joinPoint);

        assertThat(result).isEqualTo(1);
        assertThat(registry.find("jdbc.operation.duration")
                .tag("component", "JdbcOperations")
                .tag("operation", "update")
                .tag("outcome", "success")
                .timer()).isNotNull();
    }
}
