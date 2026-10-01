package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.model.InquiryRequestEntity;
import ir.jetvam.modules.inquiry.model.InquiryResponseMode;
import ir.jetvam.modules.inquiry.model.InquiryStatus;
import ir.jetvam.modules.inquiry.repository.InquiryRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns the single transactional registration path used by every inquiry response mode.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
@Service
@RequiredArgsConstructor
public class InquiryRequestRegistrationService {

    private final InquiryRequestRepository repository;
    private final InquiryDefinitionService definitionService;
    private final TimeProvider timeProvider;
    private final ObjectMapper objectMapper;

    @Transactional
    public InquirySubmissionModels.Result register(InquirySubmissionModels.Command command) {
        Preconditions.requireNonNull(command, "command");
        InquiryType inquiryType = Preconditions.requireNonNull(command.inquiryType(), "inquiryType");
        InquiryResponseMode responseMode = Preconditions.requireNonNull(command.responseMode(), "responseMode");
        definitionService.requireEnabledValidity(inquiryType);
        String nationalCode = IranianIdentifiers.normalizeNationalCode(command.nationalCode());
        Preconditions.require(IranianIdentifiers.isValidNationalCode(nationalCode), "nationalCode is invalid");
        String subjectKey = Preconditions.requireText(command.subjectKey(), "subjectKey").strip();
        String requestJson = write(Preconditions.requireNonNull(command.request(), "request"));
        CallbackConfiguration callback = callback(responseMode, command.callback());

        if (responseMode == InquiryResponseMode.ASYNC_CALLBACK) {
            Optional<InquiryRequestEntity> idempotent = repository
                    .findByCallbackTransportAndCallbackDestinationAndCallbackCorrelationId(
                            callback.transport(), callback.destination(), callback.correlationId()
                    );
            if (idempotent.isPresent()) {
                return view(idempotent.get());
            }
        }

        var now = timeProvider.now();
        Optional<InquiryRequestEntity> reusable = repository
                .findFirstByInquiryCodeAndSubjectKeyAndStatusAndValidUntilAfterOrderByCompletedAtDesc(
                        inquiryType, subjectKey, InquiryStatus.COMPLETED, now
                );
        InquiryRequestEntity request;
        if (reusable.isPresent()) {
            request = responseMode == InquiryResponseMode.SYNCHRONOUS
                    ? InquiryRequestEntity.reusedSynchronously(requestJson, reusable.get(), now)
                    : InquiryRequestEntity.reusedAsynchronously(
                            requestJson, callback.transport(), callback.destination(), callback.correlationId(),
                            reusable.get(), now
                    );
        } else {
            request = new InquiryRequestEntity(
                    inquiryType, nationalCode, subjectKey, requestJson, responseMode,
                    callback.transport(), callback.destination(), callback.correlationId(), now
            );
        }
        return view(repository.save(request));
    }

    @Transactional(readOnly = true)
    public InquirySubmissionModels.Result find(UUID requestId) {
        return view(repository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("inquiryRequest", requestId)));
    }

    private CallbackConfiguration callback(
            InquiryResponseMode responseMode,
            InquirySubmissionModels.Callback callback
    ) {
        if (responseMode == InquiryResponseMode.SYNCHRONOUS) {
            Preconditions.require(callback == null, "Synchronous inquiry must not define a callback");
            return CallbackConfiguration.NONE;
        }
        InquirySubmissionModels.Callback required = Preconditions.requireNonNull(callback, "callback");
        return new CallbackConfiguration(
                Preconditions.requireText(required.transport(), "callback.transport").strip(),
                Preconditions.requireText(required.destination(), "callback.destination").strip(),
                Preconditions.requireText(required.correlationId(), "callback.correlationId").strip()
        );
    }

    private InquirySubmissionModels.Result view(InquiryRequestEntity request) {
        return new InquirySubmissionModels.Result(
                request.getId(), request.getStatus(), readFacts(request.getResultJson()),
                request.getProviderCode(), request.getRejectionCode(),
                request.getResultMessage(), request.isCacheHit(), request.getNextAttemptAt()
        );
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Unable to persist inquiry request", exception);
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
            throw new IllegalArgumentException("Unable to read inquiry result", exception);
        }
    }

    private record CallbackConfiguration(String transport, String destination, String correlationId) {
        private static final CallbackConfiguration NONE = new CallbackConfiguration(null, null, null);
    }
}
