package ir.jetvam.modules.inquiry.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.inquiry.model.InquiryRequestEntity;
import ir.jetvam.modules.inquiry.model.InquiryStatus;
import ir.jetvam.modules.inquiry.repository.InquiryRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Persists idempotent consumer requests before any provider execution occurs.
 * The callback identity is the stable idempotency boundary across consumer retries.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultAsyncInquiryService implements AsyncInquiryService {

    private final InquiryRequestRepository requestRepository;
    private final InquiryDefinitionService definitionService;
    private final TimeProvider timeProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public UUID submit(AsyncInquiryModels.Submit command) {
        Preconditions.requireNonNull(command, "command");
        AsyncInquiryModels.Callback callback = Preconditions.requireNonNull(command.callback(), "callback");
        String transport = Preconditions.requireText(callback.transport(), "callback.transport").strip();
        String destination = Preconditions.requireText(callback.destination(), "callback.destination").strip();
        String correlationId = Preconditions.requireText(callback.correlationId(), "callback.correlationId").strip();
        InquiryType inquiryCode = InquiryType.requireDeferred(command.inquiryCode());
        definitionService.requireEnabledValidity(inquiryCode);
        return requestRepository.findByCallbackTransportAndCallbackDestinationAndCallbackCorrelationId(
                        transport, destination, correlationId
                )
                .map(InquiryRequestEntity::getId)
                .orElseGet(() -> create(command, inquiryCode, transport, destination, correlationId));
    }

    private UUID create(
            AsyncInquiryModels.Submit command,
            InquiryType inquiryCode,
            String transport,
            String destination,
            String correlationId
    ) {
        String nationalCode = IranianIdentifiers.normalizeNationalCode(command.nationalCode());
        Preconditions.require(IranianIdentifiers.isValidNationalCode(nationalCode), "nationalCode is invalid");
        String requestJson = writeRequest(inquiryCode, nationalCode);
        var reusable = requestRepository
                .findFirstByInquiryCodeAndSubjectKeyAndStatusAndValidUntilAfterOrderByCompletedAtDesc(
                        inquiryCode, nationalCode, InquiryStatus.COMPLETED, timeProvider.now()
                );
        InquiryRequestEntity request = reusable
                .map(source -> InquiryRequestEntity.reusedAsynchronously(
                        requestJson, transport, destination, correlationId, source, timeProvider.now()
                ))
                .orElseGet(() -> new InquiryRequestEntity(
                        inquiryCode, nationalCode, nationalCode, requestJson,
                        transport, destination, correlationId, timeProvider.now()
                ));
        requestRepository.save(request);
        return request.getId();
    }

    private String writeRequest(InquiryType inquiryCode, String nationalCode) {
        try {
            return objectMapper.writeValueAsString(new PersistedAsyncRequest(inquiryCode.code(), nationalCode));
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Unable to persist inquiry request", exception);
        }
    }

    private record PersistedAsyncRequest(String inquiryCode, String nationalCode) {
    }
}
