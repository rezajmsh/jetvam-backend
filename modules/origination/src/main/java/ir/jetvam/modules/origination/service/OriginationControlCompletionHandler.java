package ir.jetvam.modules.origination.service;

import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.modules.inquiry.model.AsyncInquiryStatus;
import ir.jetvam.modules.inquiry.service.AsyncInquiryModels;
import ir.jetvam.modules.inquiry.service.InquiryCompletionHandler;
import ir.jetvam.modules.origination.model.ApplicationControlEntity;
import ir.jetvam.modules.origination.model.LoanApplicationEntity;
import ir.jetvam.modules.origination.repository.ApplicationControlRepository;
import ir.jetvam.modules.origination.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Applies a terminal Inquiry outcome to its correlated application control.
 * The application lock serializes callbacks and the terminal control state makes delivery idempotent.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Component
@RequiredArgsConstructor
public class OriginationControlCompletionHandler implements InquiryCompletionHandler {

    public static final String KEY = "origination-application-control";

    private final ApplicationControlRepository controlRepository;
    private final LoanApplicationRepository applicationRepository;
    private final OriginationControlOrchestrator orchestrator;

    @Override
    public String key() {
        return KEY;
    }

    @Override
    @Transactional
    public void handle(AsyncInquiryModels.CompletionEvent event) {
        ApplicationControlEntity control = findWithApplicationLock(event.correlationId());
        if (!event.requestId().equals(control.getInquiryRequestId())) {
            throw new IllegalStateException("Inquiry callback does not match the application control request");
        }
        if (control.terminal()) {
            return;
        }
        LoanApplicationEntity application = control.getApplication();
        if (event.status() == AsyncInquiryStatus.COMPLETED) {
            orchestrator.complete(application, control, event.facts());
        } else if (event.status() == AsyncInquiryStatus.REJECTED) {
            orchestrator.reject(
                    application,
                    control,
                    event.message() == null ? control.getFailureMessage() : event.message()
            );
        } else if (event.status() == AsyncInquiryStatus.FAILED) {
            orchestrator.technicalFailure(application, control, event.message());
        } else {
            throw new IllegalArgumentException("Inquiry callback outcome is not terminal: " + event.status());
        }
    }

    private ApplicationControlEntity findWithApplicationLock(String correlationId) {
        UUID controlId;
        try {
            controlId = UUID.fromString(correlationId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid Origination control correlation id", exception);
        }
        UUID applicationId = controlRepository.findApplicationId(controlId)
                .orElseThrow(() -> new ResourceNotFoundException("applicationControl", controlId));
        LoanApplicationEntity application = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("loanApplication", applicationId));
        return application.getControls().stream()
                .filter(control -> controlId.equals(control.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("applicationControl", controlId));
    }
}
