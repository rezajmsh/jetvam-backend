package ir.jetvam.modules.inquiry.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Persists a consumer-neutral asynchronous inquiry, provider affinity and callback delivery state.
 * Provider polling and callback retries remain durable across process restarts and job nodes.
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
    private String inquiryCode;

    @Column(name = "national_code", nullable = false, length = 10)
    private String nationalCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AsyncInquiryStatus status;

    @Column(name = "provider_code", length = 100)
    private String providerCode;

    @Column(name = "external_tracking_code", length = 150)
    private String externalTrackingCode;

    @Column(name = "facts_json", columnDefinition = "text")
    private String factsJson;

    @Column(name = "rejection_code", length = 100)
    private String rejectionCode;

    @Column(name = "result_message", length = 1000)
    private String resultMessage;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "callback_transport", nullable = false, length = 50)
    private String callbackTransport;

    @Column(name = "callback_destination", nullable = false, length = 150)
    private String callbackDestination;

    @Column(name = "callback_correlation_id", nullable = false, length = 150)
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
            String inquiryCode,
            String nationalCode,
            String callbackTransport,
            String callbackDestination,
            String callbackCorrelationId,
            Instant now
    ) {
        this.inquiryCode = normalized(inquiryCode, "inquiryCode");
        this.nationalCode = normalized(nationalCode, "nationalCode");
        this.callbackTransport = normalized(callbackTransport, "callbackTransport");
        this.callbackDestination = normalized(callbackDestination, "callbackDestination");
        this.callbackCorrelationId = normalized(callbackCorrelationId, "callbackCorrelationId");
        this.status = AsyncInquiryStatus.QUEUED;
        this.callbackStatus = InquiryCallbackStatus.NOT_READY;
        this.nextAttemptAt = Preconditions.requireNonNull(now, "now");
    }

    public void start(Instant now) {
        Preconditions.require(status == AsyncInquiryStatus.QUEUED
                        || status == AsyncInquiryStatus.WAITING_PROVIDER,
                "Inquiry request is not ready");
        status = AsyncInquiryStatus.PROCESSING;
        processingStartedAt = Preconditions.requireNonNull(now, "now");
        attemptCount++;
    }

    public void waitForProvider(String provider, String trackingCode, Instant nextPollAt) {
        requireProcessing();
        providerCode = normalized(provider, "providerCode");
        externalTrackingCode = normalized(trackingCode, "externalTrackingCode");
        nextAttemptAt = Preconditions.requireNonNull(nextPollAt, "nextPollAt");
        processingStartedAt = null;
        status = AsyncInquiryStatus.WAITING_PROVIDER;
    }

    public void complete(String provider, String trackingCode, String facts, Instant now) {
        requireProcessing();
        providerCode = provider;
        externalTrackingCode = trackingCode;
        factsJson = Preconditions.requireText(facts, "factsJson");
        terminal(AsyncInquiryStatus.COMPLETED, now);
    }

    public void reject(String provider, String trackingCode, String code, String message, Instant now) {
        requireProcessing();
        providerCode = provider;
        externalTrackingCode = trackingCode;
        rejectionCode = code;
        resultMessage = limited(message, "Inquiry provider rejected the request");
        terminal(AsyncInquiryStatus.REJECTED, now);
    }

    public void retry(String error, Instant nextAttempt) {
        resultMessage = limited(error, "Inquiry execution failed");
        nextAttemptAt = Preconditions.requireNonNull(nextAttempt, "nextAttempt");
        processingStartedAt = null;
        status = AsyncInquiryStatus.QUEUED;
    }

    public void fail(String error, Instant now) {
        resultMessage = limited(error, "Inquiry execution failed");
        terminal(AsyncInquiryStatus.FAILED, now);
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

    public boolean polling() {
        return externalTrackingCode != null;
    }

    private void terminal(AsyncInquiryStatus terminalStatus, Instant now) {
        status = terminalStatus;
        processingStartedAt = null;
        callbackStatus = InquiryCallbackStatus.PENDING;
        callbackNextAttemptAt = Preconditions.requireNonNull(now, "now");
    }

    private void requireProcessing() {
        Preconditions.require(status == AsyncInquiryStatus.PROCESSING, "Inquiry request is not processing");
    }

    private static String normalized(String value, String field) {
        return Preconditions.requireText(value, field).strip();
    }

    private static String limited(String value, String fallback) {
        String resolved = value == null || value.isBlank() ? fallback : value.strip();
        return resolved.substring(0, Math.min(1000, resolved.length()));
    }
}
