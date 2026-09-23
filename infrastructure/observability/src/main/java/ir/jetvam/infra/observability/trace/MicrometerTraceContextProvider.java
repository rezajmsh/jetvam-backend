package ir.jetvam.infra.observability.trace;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Reads active trace identifiers from Micrometer Tracing.
 * It adapts framework tracing to the common trace-context contract.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@RequiredArgsConstructor
public class MicrometerTraceContextProvider implements TraceContextProvider {

    private final ObjectProvider<Tracer> tracerProvider;

    @Override
    public TraceContext current() {
        Tracer tracer = tracerProvider.getIfAvailable();
        Span span = tracer == null ? null : tracer.currentSpan();
        if (span == null) {
            return TraceContext.empty();
        }
        var context = span.context();
        return new TraceContext(context.traceId(), context.spanId(), context.sampled());
    }
}
