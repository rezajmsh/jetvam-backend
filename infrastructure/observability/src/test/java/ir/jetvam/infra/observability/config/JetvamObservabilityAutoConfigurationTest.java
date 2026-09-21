package ir.jetvam.infra.observability.config;

import ir.jetvam.infra.observability.audit.AuditLogger;
import ir.jetvam.infra.observability.logging.OtelEventLogger;
import ir.jetvam.infra.observability.metrics.InfrastructureMetrics;
import ir.jetvam.infra.observability.trace.TraceContextProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JetvamObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JetvamObservabilityAutoConfiguration.class));

    @Test
    void createsCoreObservabilityServicesByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OtelEventLogger.class);
            assertThat(context).hasSingleBean(InfrastructureMetrics.class);
            assertThat(context).hasSingleBean(TraceContextProvider.class);
            assertThat(context).hasSingleBean(AuditLogger.class);
            assertThat(context).hasSingleBean(JetvamObservabilityProperties.class);
        });
    }

    @Test
    void canDisableTheWholeInfrastructure() {
        contextRunner
                .withPropertyValues("jetvam.observability.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OtelEventLogger.class);
                    assertThat(context).doesNotHaveBean(AuditLogger.class);
                });
    }
}
