package ir.jetvam.infra.observability.trace;

public interface TraceContextProvider {

    TraceContext current();
}
