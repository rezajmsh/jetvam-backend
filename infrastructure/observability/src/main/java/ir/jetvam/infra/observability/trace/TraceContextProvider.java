package ir.jetvam.infra.observability.trace;

/**
 * Defines access to the current distributed tracing context.
 * Callers remain independent from the tracing implementation.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public interface TraceContextProvider {

    TraceContext current();
}
