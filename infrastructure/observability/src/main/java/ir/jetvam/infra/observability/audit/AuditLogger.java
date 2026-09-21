package ir.jetvam.infra.observability.audit;

public interface AuditLogger {

    void record(AuditEvent event);
}
