package ir.jetvam.modules.inquiry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.modules.inquiry.model.AsyncInquiryStatus;
import ir.jetvam.modules.inquiry.model.InquiryCallbackStatus;
import ir.jetvam.modules.inquiry.model.InquiryRequestEntity;
import ir.jetvam.modules.inquiry.repository.InquiryRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns short database phases around provider execution and callback delivery.
 * External providers and callback transports are never invoked inside these transactions.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class InquiryWorkTransactionService {

    private static final Collection<AsyncInquiryStatus> CLAIMABLE = List.of(
            AsyncInquiryStatus.QUEUED,
            AsyncInquiryStatus.WAITING_PROVIDER
    );
    private static final int MAX_ATTEMPTS = 8;
    private static final int MAX_CALLBACK_ATTEMPTS = 10;
    private static final Duration LEASE = Duration.ofMinutes(10);

    private final InquiryRequestRepository requestRepository;
    private final TimeProvider timeProvider;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AsyncInquiryModels.WorkItem> prepareNext() {
        return requestRepository.findDue(CLAIMABLE, timeProvider.now(), PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(request -> {
                    request.start(timeProvider.now());
                    return new AsyncInquiryModels.WorkItem(
                            request.getId(), request.getInquiryCode(), request.getNationalCode(),
                            request.getProviderCode(), request.getExternalTrackingCode()
                    );
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID requestId, DeferredInquiryModels.Result result) {
        InquiryRequestEntity request = find(requestId);
        if (result.status() == DeferredInquiryStatus.PENDING) {
            request.waitForProvider(
                    result.providerCode(), result.externalTrackingCode(),
                    timeProvider.now().plusSeconds(Math.max(30, result.retryAfterSeconds()))
            );
        } else if (result.status() == DeferredInquiryStatus.REJECTED) {
            request.reject(
                    result.providerCode(), result.externalTrackingCode(), result.rejectionCode(),
                    result.rejectionMessage(), timeProvider.now()
            );
        } else {
            request.complete(
                    result.providerCode(), result.externalTrackingCode(), writeFacts(result.facts()),
                    timeProvider.now()
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID requestId, RuntimeException failure) {
        InquiryRequestEntity request = find(requestId);
        String message = message(failure);
        if (request.getAttemptCount() >= MAX_ATTEMPTS) {
            request.fail(message, timeProvider.now());
        } else {
            request.retry(message, timeProvider.now().plus(retryDelay(request.getAttemptCount())));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AsyncInquiryModels.CallbackWork> prepareCallback() {
        return requestRepository.findCallbackDue(
                        InquiryCallbackStatus.PENDING, timeProvider.now(), PageRequest.of(0, 1)
                ).stream().findFirst()
                .map(request -> {
                    request.startCallback(timeProvider.now());
                    return callbackWork(request);
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void callbackDelivered(UUID requestId) {
        find(requestId).callbackDelivered();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void callbackFailed(UUID requestId, RuntimeException failure) {
        InquiryRequestEntity request = find(requestId);
        if (request.getCallbackAttemptCount() >= MAX_CALLBACK_ATTEMPTS) {
            request.failCallback(message(failure));
        } else {
            request.retryCallback(
                    message(failure),
                    timeProvider.now().plus(retryDelay(request.getCallbackAttemptCount()))
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverStale() {
        requestRepository.findStale(
                AsyncInquiryStatus.PROCESSING,
                timeProvider.now().minus(LEASE),
                PageRequest.of(0, 100)
        ).forEach(request -> request.retry("Recovered after processing lease expired", timeProvider.now()));
        requestRepository.findStaleCallbacks(
                InquiryCallbackStatus.PROCESSING,
                timeProvider.now().minus(LEASE),
                PageRequest.of(0, 100)
        ).forEach(request -> request.retryCallback(
                "Recovered after callback lease expired",
                timeProvider.now()
        ));
    }

    private AsyncInquiryModels.CallbackWork callbackWork(InquiryRequestEntity request) {
        AsyncInquiryModels.Callback callback = new AsyncInquiryModels.Callback(
                request.getCallbackTransport(), request.getCallbackDestination(), request.getCallbackCorrelationId()
        );
        AsyncInquiryModels.CompletionEvent event = new AsyncInquiryModels.CompletionEvent(
                request.getId(), request.getInquiryCode(), request.getStatus(), readFacts(request.getFactsJson()),
                request.getRejectionCode(), request.getResultMessage(), request.getCallbackCorrelationId()
        );
        return new AsyncInquiryModels.CallbackWork(callback, event);
    }

    private InquiryRequestEntity find(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("inquiryRequest", requestId));
    }

    private String writeFacts(Map<String, String> facts) {
        try {
            return objectMapper.writeValueAsString(facts);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to persist inquiry facts", exception);
        }
    }

    private Map<String, String> readFacts(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to read inquiry facts", exception);
        }
    }

    private static Duration retryDelay(int attemptCount) {
        return Duration.ofSeconds(Math.min(900, 30L << Math.min(attemptCount, 5)));
    }

    private static String message(RuntimeException failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }
}
