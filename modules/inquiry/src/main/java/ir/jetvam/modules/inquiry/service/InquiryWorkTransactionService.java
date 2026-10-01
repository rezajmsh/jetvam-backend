package ir.jetvam.modules.inquiry.service;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.modules.inquiry.model.InquiryCallbackStatus;
import ir.jetvam.modules.inquiry.execution.InquiryExecutionContext;
import ir.jetvam.modules.inquiry.execution.InquiryExecutionResult;
import ir.jetvam.modules.inquiry.execution.InquiryExecutionStatus;
import ir.jetvam.modules.inquiry.model.InquiryRequestEntity;
import ir.jetvam.modules.inquiry.model.InquiryStatus;
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

    private static final Collection<InquiryStatus> CLAIMABLE = List.of(
            InquiryStatus.QUEUED,
            InquiryStatus.WAITING_PROVIDER
    );
    private static final int MAX_ATTEMPTS = 8;
    private static final int MAX_CALLBACK_ATTEMPTS = 10;
    private static final Duration LEASE = Duration.ofMinutes(10);

    private final InquiryRequestRepository requestRepository;
    private final InquiryDefinitionService definitionService;
    private final TimeProvider timeProvider;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AsyncInquiryModels.WorkItem> prepareNext() {
        return requestRepository.findDue(CLAIMABLE, timeProvider.now(), PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(this::claim);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<AsyncInquiryModels.WorkItem> prepare(UUID requestId) {
        InquiryRequestEntity request = requestRepository.findLockedById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("inquiryRequest", requestId));
        boolean claimable = CLAIMABLE.contains(request.getStatus())
                && request.getNextAttemptAt() != null
                && !request.getNextAttemptAt().isAfter(timeProvider.now());
        return claimable ? Optional.of(claim(request)) : Optional.empty();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void complete(UUID requestId, InquiryExecutionResult result) {
        InquiryRequestEntity request = find(requestId);
        if (result.status() == InquiryExecutionStatus.PENDING) {
            request.waitForProvider(
                    result.context().providerCode(), writeContext(result.context()),
                    timeProvider.now().plusSeconds(Math.max(30, result.retryAfterSeconds()))
            );
        } else if (result.status() == InquiryExecutionStatus.REJECTED) {
            request.reject(
                    result.context().providerCode(), writeContext(result.context()), result.rejectionCode(),
                    result.rejectionMessage(), timeProvider.now()
            );
        } else {
            var now = timeProvider.now();
            request.complete(
                    result.context().providerCode(), writeContext(result.context()), writeFacts(result.facts()),
                    now,
                    now.plus(definitionService.validity(request.getInquiryCode()))
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
        recoverStaleExecutionsInternal();
        recoverStaleCallbacksInternal();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverStaleExecutions() {
        recoverStaleExecutionsInternal();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recoverStaleCallbacks() {
        recoverStaleCallbacksInternal();
    }

    private void recoverStaleExecutionsInternal() {
        requestRepository.findStale(
                InquiryStatus.PROCESSING,
                timeProvider.now().minus(LEASE),
                PageRequest.of(0, 100)
        ).forEach(request -> request.retry("Recovered after processing lease expired", timeProvider.now()));
    }

    private void recoverStaleCallbacksInternal() {
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
                request.getId(), request.getInquiryCode(), request.getNationalCode(), consumerReference(request),
                request.getProviderCode(), request.getStatus(), readFacts(request.getResultJson()),
                request.getRejectionCode(), request.getResultMessage(), request.getCallbackCorrelationId()
        );
        return new AsyncInquiryModels.CallbackWork(callback, event);
    }

    private AsyncInquiryModels.WorkItem claim(InquiryRequestEntity request) {
        request.start(timeProvider.now());
        return new AsyncInquiryModels.WorkItem(
                request.getId(), request.getInquiryCode(), request.getNationalCode(),
                consumerReference(request), request.getRequestJson(), request.getResponseMode(),
                readContext(request.getProviderCode(), request.getExecutionContextJson())
        );
    }

    private static String consumerReference(InquiryRequestEntity request) {
        return request.getCallbackCorrelationId() == null || request.getCallbackCorrelationId().isBlank()
                ? request.getSubjectKey()
                : request.getCallbackCorrelationId();
    }

    private InquiryRequestEntity find(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("inquiryRequest", requestId));
    }

    private String writeFacts(Map<String, String> facts) {
        try {
            return objectMapper.writeValueAsString(facts);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Unable to persist inquiry facts", exception);
        }
    }

    private String writeContext(InquiryExecutionContext context) {
        try {
            return objectMapper.writeValueAsString(context.data());
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Unable to persist inquiry execution context", exception);
        }
    }

    private InquiryExecutionContext readContext(String providerCode, String json) {
        if (json == null || json.isBlank()) {
            return new InquiryExecutionContext(providerCode, Map.of());
        }
        try {
            Map<String, String> data = objectMapper.readValue(json, new TypeReference<>() {
            });
            return new InquiryExecutionContext(providerCode, data);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Unable to read inquiry execution context", exception);
        }
    }

    private Map<String, String> readFacts(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JacksonException exception) {
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
