package ir.jetvam.infra.observability.audit;

/**
 * Defines the application-facing contract for publishing audit events.
 * Business modules do not depend on a concrete logging backend.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public interface AuditLogger {

    void record(AuditEvent event);
}
