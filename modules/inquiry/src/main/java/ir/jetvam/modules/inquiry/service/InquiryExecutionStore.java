package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.modules.inquiry.model.InquiryRequestEntity;
import ir.jetvam.modules.inquiry.model.InquiryStatus;
import ir.jetvam.modules.inquiry.repository.InquiryRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns the short database phases surrounding synchronous provider calls.
 * It records every invocation and creates explicit cache-hit rows linked to the reused result.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
@Service
@RequiredArgsConstructor
public class InquiryExecutionStore {

    private final InquiryRequestRepository requestRepository;
    private final InquiryDefinitionService definitionService;
    private final TimeProvider timeProvider;

    @Transactional
    public Preparation prepare(
            String inquiryCode,
            String nationalCode,
            String subjectKey,
            String requestJson
    ) {
        Duration validity = definitionService.requireEnabledValidity(inquiryCode);
        Instant now = timeProvider.now();
        Optional<InquiryRequestEntity> reusable = requestRepository
                .findFirstByInquiryCodeAndSubjectKeyAndStatusAndValidUntilAfterOrderByCompletedAtDesc(
                        inquiryCode, subjectKey, InquiryStatus.COMPLETED, now
                );
        if (reusable.isPresent()) {
            InquiryRequestEntity cached = reusable.get();
            InquiryRequestEntity audit = requestRepository.save(
                    InquiryRequestEntity.reusedSynchronously(requestJson, cached, now)
            );
            return new Preparation(audit.getId(), cached.getResultJson(), true, validity);
        }
        InquiryRequestEntity request = requestRepository.save(InquiryRequestEntity.synchronous(
                inquiryCode, nationalCode, subjectKey, requestJson, now
        ));
        return new Preparation(request.getId(), null, false, validity);
    }

    @Transactional
    public void complete(
            UUID requestId,
            String providerCode,
            String trackingCode,
            String resultJson,
            Duration validity
    ) {
        Instant now = timeProvider.now();
        find(requestId).complete(providerCode, trackingCode, resultJson, now, now.plus(validity));
    }

    @Transactional
    public void fail(UUID requestId, RuntimeException failure) {
        find(requestId).fail(message(failure), timeProvider.now());
    }

    private InquiryRequestEntity find(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("inquiryRequest", requestId));
    }

    private static String message(RuntimeException failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }

    public record Preparation(UUID requestId, String cachedResultJson, boolean cacheHit, Duration validity) {
    }
}
