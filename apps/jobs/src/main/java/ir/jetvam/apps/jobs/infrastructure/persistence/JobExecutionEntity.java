package ir.jetvam.apps.jobs.infrastructure.persistence;

import ir.jetvam.apps.jobs.infrastructure.model.JobExecutionStatus;
import ir.jetvam.apps.jobs.infrastructure.model.JobTriggerType;
import ir.jetvam.apps.jobs.infrastructure.handler.JobResult;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

/**
 * Immutable-snapshot history record for one scheduled or manual execution.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
@Entity
@Table(name = "job_execution")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobExecutionEntity extends AbstractAuditableUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_definition_id", nullable = false, updatable = false)
    private JobDefinitionEntity definition;

    @Column(name = "job_code", nullable = false, updatable = false, length = 100)
    private String jobCode;

    @Column(name = "handler_key", nullable = false, updatable = false, length = 150)
    private String handlerKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, updatable = false, length = 20)
    private JobTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobExecutionStatus status;

    @Column(name = "requested_by", updatable = false, length = 200)
    private String requestedBy;

    @Column(name = "scheduled_at", updatable = false)
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "processed_count", nullable = false)
    private long processedCount;

    @Column(name = "succeeded_count", nullable = false)
    private long succeededCount;

    @Column(name = "failed_count", nullable = false)
    private long failedCount;

    @Column(name = "result_summary", columnDefinition = "text")
    private String resultSummary;

    @Column(name = "error_type", length = 250)
    private String errorType;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "error_stack_trace", columnDefinition = "text")
    private String errorStackTrace;

    public JobExecutionEntity(
            JobDefinitionEntity definition,
            JobTriggerType triggerType,
            JobExecutionStatus status,
            String requestedBy,
            Instant scheduledAt
    ) {
        this.definition = definition;
        this.jobCode = definition.getCode();
        this.handlerKey = definition.getHandlerKey();
        this.triggerType = triggerType;
        this.status = status;
        this.requestedBy = requestedBy;
        this.scheduledAt = scheduledAt;
    }

    public void markRunning(Instant now) {
        status = JobExecutionStatus.RUNNING;
        startedAt = now;
    }

    public void markSucceeded(Instant now, JobResult result) {
        status = JobExecutionStatus.SUCCEEDED;
        finishedAt = now;
        durationMs = elapsedMillis(now);
        processedCount = result.processedCount();
        succeededCount = result.succeededCount();
        failedCount = result.failedCount();
        resultSummary = truncate(result.summary(), 4000);
        clearError();
    }

    public void markFailed(Instant now, Throwable error, String stackTrace) {
        status = JobExecutionStatus.FAILED;
        if (startedAt == null) {
            startedAt = now;
        }
        finishedAt = now;
        durationMs = elapsedMillis(now);
        if (processedCount == 0) {
            processedCount = 1;
            failedCount = 1;
        }
        errorType = truncate(error.getClass().getName(), 250);
        errorMessage = truncate(error.getMessage(), 4000);
        errorStackTrace = truncate(stackTrace, 16000);
    }

    private long elapsedMillis(Instant now) {
        return Math.max(0, Duration.between(startedAt, now).toMillis());
    }

    private void clearError() {
        errorType = null;
        errorMessage = null;
        errorStackTrace = null;
    }

    private static String truncate(String value, int maximumLength) {
        return value == null || value.length() <= maximumLength ? value : value.substring(0, maximumLength);
    }
}
