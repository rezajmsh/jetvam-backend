package ir.jetvam.infra.observability.trace;

public record TraceContext(String traceId, String spanId, boolean sampled) {

    public static TraceContext empty() {
        return new TraceContext(null, null, false);
    }

    public boolean present() {
        return traceId != null && !traceId.isBlank();
    }
}
