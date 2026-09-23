package ir.jetvam.infra.observability.audit;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.Objects;

/**
 * Carries an immutable security or business audit event.
 * Structured attributes support trace correlation and compliance storage.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@Getter
public final class AuditEvent {

    private final String action;
    private final String outcome;
    private final String actorType;
    private final String actorId;
    private final String subjectType;
    private final String subjectId;
    private final Map<String, Object> attributes;

    @Builder
    public AuditEvent(
            String action,
            String outcome,
            String actorType,
            String actorId,
            String subjectType,
            String subjectId,
            Map<String, ?> attributes
    ) {
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.outcome = Objects.requireNonNull(outcome, "outcome must not be null");
        this.actorType = actorType;
        this.actorId = actorId;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
