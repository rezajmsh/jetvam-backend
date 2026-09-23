package ir.jetvam.infra.web.config;

import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.infra.core.config.JetvamCoreAutoConfiguration;
import ir.jetvam.infra.observability.config.JetvamObservabilityAutoConfiguration;
import ir.jetvam.infra.observability.config.JetvamObservabilityProperties;
import ir.jetvam.infra.observability.logging.OtelEventLogger;
import ir.jetvam.infra.observability.metrics.InfrastructureMetrics;
import ir.jetvam.infra.observability.trace.TraceContextProvider;
import ir.jetvam.infra.web.api.ApiResponseFactory;
import ir.jetvam.infra.web.api.JetvamResponseBodyAdvice;
import ir.jetvam.infra.web.error.DefaultExceptionHttpStatusMapper;
import ir.jetvam.infra.web.error.ExceptionHttpStatusMapper;
import ir.jetvam.infra.web.error.GlobalExceptionHandler;
import ir.jetvam.infra.web.observability.HttpServerObservabilityFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

/**
 * Auto-configures the jetvam web infrastructure.
 * Applications activate reusable beans through classpath and property conditions.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@AutoConfiguration(after = {
        JetvamCoreAutoConfiguration.class,
        JetvamObservabilityAutoConfiguration.class
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties({JetvamWebProperties.class, JetvamObservabilityProperties.class})
public class JetvamWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    OtelEventLogger jetvamWebFallbackEventLogger() {
        return new OtelEventLogger();
    }

    @Bean
    @ConditionalOnMissingBean
    TraceContextProvider jetvamWebFallbackTraceContextProvider() {
        return ir.jetvam.infra.observability.trace.TraceContext::empty;
    }

    @Bean
    @ConditionalOnMissingBean
    ApiResponseFactory jetvamApiResponseFactory(
            TimeProvider timeProvider,
            TraceContextProvider traceContextProvider
    ) {
        return new ApiResponseFactory(timeProvider, traceContextProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    ExceptionHttpStatusMapper jetvamExceptionHttpStatusMapper() {
        return new DefaultExceptionHttpStatusMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    JetvamResponseBodyAdvice jetvamResponseBodyAdvice(
            ApiResponseFactory responseFactory,
            JetvamWebProperties properties
    ) {
        return new JetvamResponseBodyAdvice(responseFactory, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    GlobalExceptionHandler jetvamGlobalExceptionHandler(
            ExceptionHttpStatusMapper statusMapper,
            ApiResponseFactory responseFactory,
            ObjectProvider<MessageSource> messageSource,
            JetvamWebProperties properties,
            JetvamObservabilityProperties observabilityProperties,
            OtelEventLogger eventLogger
    ) {
        return new GlobalExceptionHandler(
                statusMapper,
                responseFactory,
                messageSource,
                properties,
                observabilityProperties,
                eventLogger
        );
    }

    @Bean
    @ConditionalOnExpression(
            "${jetvam.observability.enabled:true} and ${jetvam.observability.http.server.enabled:true}"
    )
    FilterRegistrationBean<HttpServerObservabilityFilter> jetvamHttpServerObservabilityFilter(
            JetvamObservabilityProperties properties,
            OtelEventLogger eventLogger,
            InfrastructureMetrics metrics
    ) {
        var registration = new FilterRegistrationBean<>(
                new HttpServerObservabilityFilter(properties, eventLogger, metrics)
        );
        registration.setName("jetvamHttpServerObservabilityFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
        return registration;
    }

}
