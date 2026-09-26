package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.model.InquiryRequestEntity;
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
    private final TimeProvider timeProvider;

    @Override
    @Transactional
    public UUID submit(AsyncInquiryModels.Submit command) {
        Preconditions.requireNonNull(command, "command");
        AsyncInquiryModels.Callback callback = Preconditions.requireNonNull(command.callback(), "callback");
        String transport = Preconditions.requireText(callback.transport(), "callback.transport").strip();
        String destination = Preconditions.requireText(callback.destination(), "callback.destination").strip();
        String correlationId = Preconditions.requireText(callback.correlationId(), "callback.correlationId").strip();
        return requestRepository.findByCallbackTransportAndCallbackDestinationAndCallbackCorrelationId(
                        transport, destination, correlationId
                )
                .map(InquiryRequestEntity::getId)
                .orElseGet(() -> create(command, transport, destination, correlationId));
    }

    private UUID create(
            AsyncInquiryModels.Submit command,
            String transport,
            String destination,
            String correlationId
    ) {
        String nationalCode = IranianIdentifiers.normalizeNationalCode(command.nationalCode());
        Preconditions.require(IranianIdentifiers.isValidNationalCode(nationalCode), "nationalCode is invalid");
        InquiryRequestEntity request = requestRepository.save(new InquiryRequestEntity(
                command.inquiryCode(), nationalCode, transport, destination, correlationId, timeProvider.now()
        ));
        return request.getId();
    }
}
