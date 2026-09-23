package ir.jetvam.apps.jobs.infrastructure.persistence;

import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Persistent operational definition of a job and its schedule. */
@Entity
@Table(name = "job_definition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobDefinitionEntity extends AbstractAuditableUuidEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "handler_key", nullable = false, length = 150)
    private String handlerKey;

    @Column(name = "cron_expression", nullable = false, length = 120)
    private String cronExpression;

    @Column(name = "time_zone", nullable = false, length = 80)
    private String timeZone;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public JobDefinitionEntity(
            String code,
            String displayName,
            String description,
            String handlerKey,
            String cronExpression,
            String timeZone,
            boolean enabled
    ) {
        update(code, displayName, description, handlerKey, cronExpression, timeZone, enabled);
    }

    public void update(
            String code,
            String displayName,
            String description,
            String handlerKey,
            String cronExpression,
            String timeZone,
            boolean enabled
    ) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
        this.handlerKey = handlerKey;
        this.cronExpression = cronExpression;
        this.timeZone = timeZone;
        this.enabled = enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
