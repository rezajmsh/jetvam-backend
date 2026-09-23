package ir.jetvam.infra.web.error;

import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.time.ClockTimeProvider;
import ir.jetvam.infra.observability.logging.OtelEventLogger;
import ir.jetvam.infra.observability.config.JetvamObservabilityProperties;
import ir.jetvam.infra.observability.trace.TraceContext;
import ir.jetvam.infra.web.api.ApiResponseFactory;
import ir.jetvam.infra.web.config.JetvamWebProperties;
import ir.jetvam.infra.web.request.WebRequestAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the behavior of global exception handler.
 * The tests protect the shared contract and its important edge cases.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

class GlobalExceptionHandlerTest {

    @Test
    void mapsDomainExceptionToTheUnifiedErrorEnvelope() {
        var responseFactory = new ApiResponseFactory(
                new ClockTimeProvider(Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC)),
                TraceContext::empty
        );
        var beanFactory = new DefaultListableBeanFactory();
        var handler = new GlobalExceptionHandler(
                new DefaultExceptionHttpStatusMapper(),
                responseFactory,
                beanFactory.getBeanProvider(MessageSource.class),
                new JetvamWebProperties(),
                new JetvamObservabilityProperties(),
                new OtelEventLogger()
        );
        var request = new MockHttpServletRequest("GET", "/api/customers/42");
        request.setAttribute(WebRequestAttributes.REQUEST_ID, "request-123");

        var response = handler.handleJetvamException(
                new ResourceNotFoundException("customer", 42),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo("COMMON.RESOURCE_NOT_FOUND");
        assertThat(response.getBody().error().details()).containsEntry("resourceType", "customer");
        assertThat(response.getBody().meta().requestId()).isEqualTo("request-123");
    }
}
