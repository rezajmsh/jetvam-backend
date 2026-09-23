package ir.jetvam.infra.observability.config;

import ch.qos.logback.classic.LoggerContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import ir.jetvam.infra.observability.audit.AuditLogger;
import ir.jetvam.infra.observability.audit.DefaultAuditLogger;
import ir.jetvam.infra.observability.logging.OtelEventLogger;
import ir.jetvam.infra.observability.metrics.InfrastructureMetrics;
import ir.jetvam.infra.observability.repository.RepositoryObservabilityAspect;
import ir.jetvam.infra.observability.repository.JdbcOperationsObservabilityAspect;
import ir.jetvam.infra.observability.trace.MicrometerTraceContextProvider;
import ir.jetvam.infra.observability.trace.TraceContextProvider;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;

/**
 * Auto-configures the jetvam observability infrastructure.
 * Applications activate reusable beans through classpath and property conditions.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@AutoConfiguration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableConfigurationProperties(JetvamObservabilityProperties.class)
@ConditionalOnProperty(prefix = "jetvam.observability", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JetvamObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    OtelEventLogger jetvamOtelEventLogger() {
        return new OtelEventLogger();
    }

    @Bean
    @ConditionalOnMissingBean
    InfrastructureMetrics jetvamInfrastructureMetrics(ObjectProvider<MeterRegistry> meterRegistry) {
        return new InfrastructureMetrics(meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    TraceContextProvider jetvamTraceContextProvider(ObjectProvider<Tracer> tracer) {
        return new MicrometerTraceContextProvider(tracer);
    }

    @Bean
    @ConditionalOnMissingBean
    AuditLogger jetvamAuditLogger(
            OtelEventLogger eventLogger,
            InfrastructureMetrics metrics,
            TraceContextProvider traceContextProvider,
            JetvamObservabilityProperties properties
    ) {
        return new DefaultAuditLogger(eventLogger, metrics, traceContextProvider, properties);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.data.repository.Repository")
    @ConditionalOnProperty(
            prefix = "jetvam.observability.repository",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    RepositoryObservabilityAspect jetvamRepositoryObservabilityAspect(
            ObjectProvider<Tracer> tracer,
            InfrastructureMetrics metrics,
            OtelEventLogger eventLogger,
            JetvamObservabilityProperties properties
    ) {
        return new RepositoryObservabilityAspect(
                tracer.getIfAvailable(() -> Tracer.NOOP),
                metrics,
                eventLogger,
                properties
        );
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.jdbc.core.JdbcOperations")
    @ConditionalOnProperty(
            prefix = "jetvam.observability.jdbc",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    JdbcOperationsObservabilityAspect jetvamJdbcOperationsObservabilityAspect(
            ObjectProvider<Tracer> tracer,
            InfrastructureMetrics metrics,
            OtelEventLogger eventLogger,
            JetvamObservabilityProperties properties
    ) {
        return new JdbcOperationsObservabilityAspect(
                tracer.getIfAvailable(() -> Tracer.NOOP),
                metrics,
                eventLogger,
                properties
        );
    }

    @Bean
    InitializingBean jetvamOtelLoggingContext(Environment environment) {
        return () -> {
            ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
            if (loggerFactory instanceof LoggerContext context) {
                context.putProperty("service.name", environment.getProperty("spring.application.name", "jetvam"));
                context.putProperty("service.version", environment.getProperty("spring.application.version", "unknown"));
            }
        };
    }
}
