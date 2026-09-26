package ir.jetvam.modules.origination.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractUuidEntity;
import ir.jetvam.modules.product.model.PlanControlType;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Snapshots and tracks one prioritized eligibility control for a loan application.
 * An optional inquiry code makes the control asynchronous without exposing provider execution to Origination.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Entity
@Table(name = "origination_application_control")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationControlEntity extends AbstractUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplicationEntity application;

    @Column(name = "control_code", nullable = false, length = 100)
    private String controlCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_type", nullable = false, length = 40)
    private PlanControlType controlType;

    @Column(name = "minimum_value", precision = 19, scale = 4)
    private BigDecimal minimumValue;

    @Column(name = "maximum_value", precision = 19, scale = 4)
    private BigDecimal maximumValue;

    @Column(name = "source_inquiry_code", length = 100)
    private String sourceInquiryCode;

    @Column(name = "failure_message", nullable = false, length = 500)
    private String failureMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ApplicationControlStatus status;

    @Column(name = "inquiry_request_id")
    private UUID inquiryRequestId;

    @Column(name = "facts_json", columnDefinition = "text")
    private String factsJson;

    @Column(name = "observed_value", length = 500)
    private String observedValue;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

    public ApplicationControlEntity(
            LoanApplicationEntity application,
            String controlCode,
            String title,
            int priority,
            PlanControlType controlType,
            BigDecimal minimumValue,
            BigDecimal maximumValue,
            String sourceInquiryCode,
            String failureMessage
    ) {
        this.application = Preconditions.requireNonNull(application, "application");
        this.controlCode = normalized(controlCode, "controlCode");
        this.title = Preconditions.requireText(title, "title").strip();
        this.priority = Preconditions.requirePositive(priority, "priority");
        this.controlType = Preconditions.requireNonNull(controlType, "controlType");
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
        this.sourceInquiryCode = sourceInquiryCode == null || sourceInquiryCode.isBlank()
                ? null : normalized(sourceInquiryCode, "sourceInquiryCode");
        this.failureMessage = Preconditions.requireText(failureMessage, "failureMessage").strip();
        this.status = ApplicationControlStatus.WAITING_PRIORITY;
    }

    public boolean requiresInquiry() {
        return sourceInquiryCode != null;
    }

    public void blockByPayment() {
        requireWaiting();
        status = ApplicationControlStatus.BLOCKED_BY_PAYMENT;
    }

    public void releaseInquiry() {
        Preconditions.require(status == ApplicationControlStatus.WAITING_PRIORITY
                        || status == ApplicationControlStatus.BLOCKED_BY_PAYMENT,
                "Application control is not waiting for inquiry release");
        Preconditions.require(requiresInquiry(), "Application control does not require an inquiry");
        status = ApplicationControlStatus.PENDING_INQUIRY;
    }

    public void inquirySubmitted(UUID requestId) {
        Preconditions.require(status == ApplicationControlStatus.PENDING_INQUIRY,
                "Application control inquiry is not pending submission");
        inquiryRequestId = Preconditions.requireNonNull(requestId, "requestId");
        status = ApplicationControlStatus.INQUIRY_SUBMITTED;
    }

    public void pass(String observed, String facts, Instant at) {
        requireEvaluable();
        observedValue = limited(observed, 500);
        factsJson = facts;
        evaluatedAt = Preconditions.requireNonNull(at, "at");
        status = ApplicationControlStatus.PASSED;
    }

    public void fail(String observed, String facts, String message, Instant at) {
        requireEvaluable();
        observedValue = limited(observed, 500);
        factsJson = facts;
        errorMessage = limited(message == null ? failureMessage : message, 1000);
        evaluatedAt = Preconditions.requireNonNull(at, "at");
        status = ApplicationControlStatus.FAILED;
    }

    public void technicalFailure(String message, Instant at) {
        Preconditions.require(status == ApplicationControlStatus.INQUIRY_SUBMITTED,
                "Application control inquiry is not submitted");
        errorMessage = limited(message == null ? "Inquiry execution failed" : message, 1000);
        evaluatedAt = Preconditions.requireNonNull(at, "at");
        status = ApplicationControlStatus.ERROR;
    }

    public void cancel() {
        if (!terminal()) {
            status = ApplicationControlStatus.CANCELLED;
        }
    }

    public boolean terminal() {
        return status == ApplicationControlStatus.PASSED
                || status == ApplicationControlStatus.FAILED
                || status == ApplicationControlStatus.ERROR
                || status == ApplicationControlStatus.CANCELLED;
    }

    private void requireWaiting() {
        Preconditions.require(status == ApplicationControlStatus.WAITING_PRIORITY,
                "Application control is not waiting for its priority");
    }

    private void requireEvaluable() {
        Preconditions.require(status == ApplicationControlStatus.WAITING_PRIORITY
                        || status == ApplicationControlStatus.BLOCKED_BY_PAYMENT
                        || status == ApplicationControlStatus.INQUIRY_SUBMITTED,
                "Application control is not ready for evaluation");
    }

    private static String normalized(String value, String field) {
        return Preconditions.requireText(value, field).strip().toUpperCase();
    }

    private static String limited(String value, int length) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        return normalized.substring(0, Math.min(length, normalized.length()));
    }
}
