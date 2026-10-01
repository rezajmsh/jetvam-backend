package ir.jetvam.apps.jobs.infrastructure.persistence;

import ir.jetvam.apps.jobs.infrastructure.model.JobExecutionItemStatus;
import ir.jetvam.common.validation.Preconditions;
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

/**
 * Immutable business-item snapshot processed during one managed job execution.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
@Entity
@Table(name = "job_execution_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobExecutionItemEntity extends AbstractAuditableUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_execution_id", nullable = false, updatable = false)
    private JobExecutionEntity execution;

    @Column(name = "sequence_number", nullable = false, updatable = false)
    private int sequenceNumber;

    @Column(name = "item_type", nullable = false, updatable = false, length = 50)
    private String itemType;

    @Column(name = "item_key", nullable = false, updatable = false, length = 150)
    private String itemKey;

    @Column(name = "operation_code", updatable = false, length = 100)
    private String operationCode;

    @Column(name = "subject_identifier", updatable = false, length = 150)
    private String subjectIdentifier;

    @Column(name = "subject_key", updatable = false, length = 150)
    private String subjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, updatable = false, length = 20)
    private JobExecutionItemStatus status;

    @Column(name = "business_status", updatable = false, length = 80)
    private String businessStatus;

    @Column(name = "provider_code", updatable = false, length = 100)
    private String providerCode;

    @Column(name = "external_reference", updatable = false, length = 150)
    private String externalReference;

    @Column(name = "message", updatable = false, length = 1000)
    private String message;

    public JobExecutionItemEntity(
            JobExecutionEntity execution,
            int sequenceNumber,
            String itemType,
            String itemKey,
            String operationCode,
            String subjectIdentifier,
            String subjectKey,
            JobExecutionItemStatus status,
            String businessStatus,
            String providerCode,
            String externalReference,
            String message
    ) {
        this.execution = Preconditions.requireNonNull(execution, "execution");
        Preconditions.require(sequenceNumber > 0, "sequenceNumber must be positive");
        this.sequenceNumber = sequenceNumber;
        this.itemType = Preconditions.requireText(itemType, "itemType").strip();
        this.itemKey = Preconditions.requireText(itemKey, "itemKey").strip();
        this.operationCode = normalized(operationCode);
        this.subjectIdentifier = normalized(subjectIdentifier);
        this.subjectKey = normalized(subjectKey);
        this.status = Preconditions.requireNonNull(status, "status");
        this.businessStatus = normalized(businessStatus);
        this.providerCode = normalized(providerCode);
        this.externalReference = normalized(externalReference);
        this.message = limited(message, 1000);
    }

    private static String normalized(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String limited(String value, int maximumLength) {
        String normalized = normalized(value);
        return normalized == null || normalized.length() <= maximumLength
                ? normalized
                : normalized.substring(0, maximumLength);
    }
}
