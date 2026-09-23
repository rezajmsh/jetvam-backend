package ir.jetvam.infra.web.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ir.jetvam.infra.observability.config.JetvamObservabilityProperties;
import ir.jetvam.infra.observability.logging.OtelEventLogger;
import ir.jetvam.infra.observability.metrics.InfrastructureMetrics;
import ir.jetvam.infra.web.request.WebRequestAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the behavior of http server observability filter.
 * The tests protect the shared contract and its important edge cases.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

class HttpServerObservabilityFilterTest {

    @Test
    void assignsRequestIdAndRecordsLowCardinalityRouteMetric() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("meterRegistry", registry);
        var metrics = new InfrastructureMetrics(beanFactory.getBeanProvider(MeterRegistry.class));
        var properties = new JetvamObservabilityProperties();
        properties.getLogging().setEnabled(false);
        var filter = new HttpServerObservabilityFilter(properties, new OtelEventLogger(), metrics);

        var request = new MockHttpServletRequest("GET", "/api/customers/42");
        request.addHeader("X-Request-Id", "request-123");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            servletRequest.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/customers/{id}");
            ((MockHttpServletResponse) servletResponse).setStatus(200);
        });

        assertThat(request.getAttribute(WebRequestAttributes.REQUEST_ID)).isEqualTo("request-123");
        assertThat(response.getHeader("X-Request-Id")).isEqualTo("request-123");
        assertThat(registry.find("http.server.request.duration")
                .tag("route", "/api/customers/{id}")
                .tag("status", "200")
                .timer()).isNotNull();
    }
}
