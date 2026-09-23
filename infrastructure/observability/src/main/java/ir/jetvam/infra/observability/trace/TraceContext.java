package ir.jetvam.infra.observability.trace;

/**
 * Carries the active trace and span identifiers for correlation.
 * An empty value represents execution outside a sampled trace.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public record TraceContext(String traceId, String spanId, boolean sampled) {

    public static TraceContext empty() {
        return new TraceContext(null, null, false);
    }

    public boolean present() {
        return traceId != null && !traceId.isBlank();
    }
}
