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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RepositoryObservabilityAspectTest {

    @Test
    void recordsDurationOutcomeRepositoryAndOperation() throws Throwable {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("meterRegistry", registry);
        var properties = new JetvamObservabilityProperties();
        properties.getLogging().setEnabled(false);
        var aspect = new RepositoryObservabilityAspect(
                Tracer.NOOP,
                new InfrastructureMetrics(beanFactory.getBeanProvider(MeterRegistry.class)),
                new OtelEventLogger(),
                properties
        );
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringType()).thenReturn((Class) CustomerRepository.class);
        when(signature.getName()).thenReturn("findByNationalCode");
        when(joinPoint.proceed()).thenReturn("customer");

        Object result = aspect.observe(joinPoint);

        assertThat(result).isEqualTo("customer");
        assertThat(registry.find("repository.operation.duration")
                .tag("repository", "CustomerRepository")
                .tag("operation", "findByNationalCode")
                .tag("outcome", "success")
                .timer()).isNotNull();
    }

    private interface CustomerRepository {
    }
}
