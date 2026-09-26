package ir.jetvam.modules.origination.service;

import ir.jetvam.modules.inquiry.service.AsyncInquiryModels;
import ir.jetvam.modules.inquiry.service.AsyncInquiryService;
import ir.jetvam.modules.origination.model.ApplicationControlEntity;
import ir.jetvam.modules.origination.model.ApplicationControlStatus;
import ir.jetvam.modules.origination.model.LoanApplicationEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Submits the inquiry required by one released application control.
 * The control identifier is the idempotent callback correlation key understood only by Origination.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class OriginationControlInquiryService {

    private final AsyncInquiryService asyncInquiryService;

    public void submit(LoanApplicationEntity application, ApplicationControlEntity control) {
        if (control.getStatus() != ApplicationControlStatus.PENDING_INQUIRY) {
            return;
        }
        java.util.UUID requestId = asyncInquiryService.submit(new AsyncInquiryModels.Submit(
                control.getSourceInquiryCode(),
                application.getNationalCode(),
                new AsyncInquiryModels.Callback(
                        AsyncInquiryModels.SPRING_BEAN,
                        OriginationControlCompletionHandler.KEY,
                        control.getId().toString()
                )
        ));
        control.inquirySubmitted(requestId);
    }
}
