package ir.jetvam.modules.inquiry.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import ir.jetvam.infra.persistence.entity.converter.InquiryTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Persists every synchronous or asynchronous inquiry invocation and its normalized result.
 * Cache reuse, provider affinity, opaque provider context and callback retries remain durable across restarts.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Entity
@Table(name = "inquiry_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryRequestEntity extends AbstractAuditableUuidEntity {

    @Column(name = "inquiry_code", nullable = false, length = 100)
    @Convert(converter = InquiryTypeConverter.class)
    private InquiryType inquiryCode;

    @Column(name = "national_code", nullable = false, length = 10)
    private String nationalCode;

    @Column(name = "subject_key", nullable = false, length = 150)
    private String subjectKey;

    @Column(name = "request_json", nullable = false, columnDefinition = "text")
    private String requestJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private InquiryStatus status;

    @Column(name = "provider_code", length = 100)
    private String providerCode;

    @Column(name = "execution_context_json", columnDefinition = "text")
    private String executionContextJson;

    @Column(name = "result_json", columnDefinition = "text")
    private String resultJson;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "cache_hit", nullable = false)
    private boolean cacheHit;

    @Column(name = "reused_from_request_id")
    private java.util.UUID reusedFromRequestId;

    @Column(name = "rejection_code", length = 100)
    private String rejectionCode;

    @Column(name = "result_message", length = 1000)
    private String resultMessage;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_mode", nullable = false, length = 30)
    private InquiryResponseMode responseMode;

    @Column(name = "callback_transport", length = 50)
    private String callbackTransport;

    @Column(name = "callback_destination", length = 150)
    private String callbackDestination;

    @Column(name = "callback_correlation_id", length = 150)
    private String callbackCorrelationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "callback_status", nullable = false, length = 40)
    private InquiryCallbackStatus callbackStatus;

    @Column(name = "callback_attempt_count", nullable = false)
    private int callbackAttemptCount;

    @Column(name = "callback_next_attempt_at")
    private Instant callbackNextAttemptAt;

    @Column(name = "callback_processing_started_at")
    private Instant callbackProcessingStartedAt;

    @Column(name = "callback_error", length = 1000)
    private String callbackError;

    public InquiryRequestEntity(
            InquiryType inquiryCode,
            String nationalCode,
            String subjectKey,
            String requestJson,
            InquiryResponseMode responseMode,
            String callbackTransport,
            String callbackDestination,
            String callbackCorrelationId,
            Instant now
    ) {
        this.inquiryCode = Preconditions.requireNonNull(inquiryCode, "inquiryCode");
        this.nationalCode = normalized(nationalCode, "nationalCode");
        this.subjectKey = normalized(subjectKey, "subjectKey");
        this.requestJson = Preconditions.requireText(requestJson, "requestJson");
        this.responseMode = Preconditions.requireNonNull(responseMode, "responseMode");
        if (responseMode == InquiryResponseMode.ASYNC_CALLBACK) {
            this.callbackTransport = normalized(callbackTransport, "callbackTransport");
            this.callbackDestination = normalized(callbackDestination, "callbackDestination");
            this.callbackCorrelationId = normalized(callbackCorrelationId, "callbackCorrelationId");
            this.callbackStatus = InquiryCallbackStatus.NOT_READY;
        } else {
            Preconditions.require(callbackTransport == null && callbackDestination == null
                    && callbackCorrelationId == null, "Synchronous inquiry must not define a callback");
            this.callbackStatus = InquiryCallbackStatus.NOT_REQUIRED;
        }
        this.status = InquiryStatus.QUEUED;
        this.nextAttemptAt = Preconditions.requireNonNull(now, "now");
    }

    public static InquiryRequestEntity synchronous(
            InquiryType inquiryCode,
            String nationalCode,
            String subjectKey,
            String requestJson,
            Instant now
    ) {
        return new InquiryRequestEntity(
                inquiryCode, nationalCode, subjectKey, requestJson, InquiryResponseMode.SYNCHRONOUS,
                null, null, null, now
        );
    }

    public static InquiryRequestEntity reusedSynchronously(
            String requestJson,
            InquiryRequestEntity source,
            Instant now
    ) {
        InquiryRequestEntity request = synchronous(
                source.inquiryCode, source.nationalCode, source.subjectKey, requestJson, now
        );
        request.reuse(source, false, now);
        return request;
    }

    public static InquiryRequestEntity reusedAsynchronously(
            String requestJson,
            String callbackTransport,
            String callbackDestination,
            String callbackCorrelationId,
            InquiryRequestEntity source,
            Instant now
    ) {
        InquiryRequestEntity request = new InquiryRequestEntity(
                source.inquiryCode, source.nationalCode, source.subjectKey, requestJson,
                InquiryResponseMode.ASYNC_CALLBACK,
                callbackTransport, callbackDestination, callbackCorrelationId, now
        );
        request.reuse(source, true, now);
        return request;
    }

    public void start(Instant now) {
        Preconditions.require(status == InquiryStatus.QUEUED
                        || status == InquiryStatus.WAITING_PROVIDER,
                "Inquiry request is not ready");
        status = InquiryStatus.PROCESSING;
        processingStartedAt = Preconditions.requireNonNull(now, "now");
        attemptCount++;
    }

    public void waitForProvider(String provider, String executionContext, Instant nextPollAt) {
        requireProcessing();
        providerCode = normalized(provider, "providerCode");
        executionContextJson = Preconditions.requireText(executionContext, "executionContextJson");
        nextAttemptAt = Preconditions.requireNonNull(nextPollAt, "nextPollAt");
        processingStartedAt = null;
        status = InquiryStatus.WAITING_PROVIDER;
    }

    public void complete(String provider, String executionContext, String facts, Instant now) {
        complete(provider, executionContext, facts, now, now);
    }

    public void complete(
            String provider,
            String executionContext,
            String result,
            Instant now,
            Instant validUntil
    ) {
        requireProcessing();
        providerCode = provider;
        executionContextJson = executionContext;
        resultJson = Preconditions.requireText(result, "resultJson");
        completedAt = Preconditions.requireNonNull(now, "now");
        this.validUntil = Preconditions.requireNonNull(validUntil, "validUntil");
        terminal(InquiryStatus.COMPLETED, now);
    }

    public void reject(String provider, String executionContext, String code, String message, Instant now) {
        requireProcessing();
        providerCode = provider;
        executionContextJson = executionContext;
        rejectionCode = code;
        resultMessage = limited(message, "Inquiry provider rejected the request");
        terminal(InquiryStatus.REJECTED, now);
    }

    public void retry(String error, Instant nextAttempt) {
        resultMessage = limited(error, "Inquiry execution failed");
        nextAttemptAt = Preconditions.requireNonNull(nextAttempt, "nextAttempt");
        processingStartedAt = null;
        status = InquiryStatus.QUEUED;
    }

    public void fail(String error, Instant now) {
        resultMessage = limited(error, "Inquiry execution failed");
        terminal(InquiryStatus.FAILED, now);
    }

    public void startCallback(Instant now) {
        Preconditions.require(callbackStatus == InquiryCallbackStatus.PENDING, "Callback is not pending");
        callbackStatus = InquiryCallbackStatus.PROCESSING;
        callbackProcessingStartedAt = Preconditions.requireNonNull(now, "now");
        callbackAttemptCount++;
    }

    public void callbackDelivered() {
        Preconditions.require(callbackStatus == InquiryCallbackStatus.PROCESSING, "Callback is not processing");
        callbackStatus = InquiryCallbackStatus.DELIVERED;
        callbackProcessingStartedAt = null;
        callbackError = null;
    }

    public void retryCallback(String error, Instant nextAttempt) {
        callbackError = limited(error, "Inquiry callback failed");
        callbackNextAttemptAt = Preconditions.requireNonNull(nextAttempt, "nextAttempt");
        callbackProcessingStartedAt = null;
        callbackStatus = InquiryCallbackStatus.PENDING;
    }

    public void failCallback(String error) {
        callbackError = limited(error, "Inquiry callback failed");
        callbackProcessingStartedAt = null;
        callbackStatus = InquiryCallbackStatus.FAILED;
    }

    private void terminal(InquiryStatus terminalStatus, Instant now) {
        status = terminalStatus;
        processingStartedAt = null;
        if (responseMode == InquiryResponseMode.ASYNC_CALLBACK) {
            callbackStatus = InquiryCallbackStatus.PENDING;
            callbackNextAttemptAt = Preconditions.requireNonNull(now, "now");
        } else {
            callbackStatus = InquiryCallbackStatus.NOT_REQUIRED;
        }
    }

    private void reuse(InquiryRequestEntity source, boolean callbackRequired, Instant now) {
        Preconditions.require(source.status == InquiryStatus.COMPLETED, "source must be completed");
        providerCode = source.providerCode;
        executionContextJson = source.executionContextJson;
        resultJson = Preconditions.requireText(source.resultJson, "source.resultJson");
        completedAt = Preconditions.requireNonNull(now, "now");
        validUntil = Preconditions.requireNonNull(source.validUntil, "source.validUntil");
        reusedFromRequestId = Preconditions.requireNonNull(source.getId(), "source.id");
        cacheHit = true;
        responseMode = callbackRequired ? InquiryResponseMode.ASYNC_CALLBACK : InquiryResponseMode.SYNCHRONOUS;
        status = InquiryStatus.COMPLETED;
        processingStartedAt = null;
        if (callbackRequired) {
            callbackStatus = InquiryCallbackStatus.PENDING;
            callbackNextAttemptAt = now;
        } else {
            callbackStatus = InquiryCallbackStatus.NOT_REQUIRED;
            callbackNextAttemptAt = null;
        }
    }

    private void requireProcessing() {
        Preconditions.require(status == InquiryStatus.PROCESSING, "Inquiry request is not processing");
    }

    private static String normalized(String value, String field) {
        return Preconditions.requireText(value, field).strip();
    }

    private static String limited(String value, String fallback) {
        String resolved = value == null || value.isBlank() ? fallback : value.strip();
        return resolved.substring(0, Math.min(1000, resolved.length()));
    }
}
