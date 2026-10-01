package ir.jetvam.modules.origination.service;

import ir.jetvam.modules.inquiry.service.InquiryRequestService;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquirySubmissionModels;
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

    private final InquiryRequestService inquiryRequestService;

    public void submit(LoanApplicationEntity application, ApplicationControlEntity control) {
        if (control.getStatus() != ApplicationControlStatus.PENDING_INQUIRY) {
            return;
        }
        java.util.UUID requestId = inquiryRequestService.submit(InquirySubmissionModels.Command.asynchronous(
                control.getSourceInquiryCode(),
                application.getNationalCode(),
                application.getNationalCode(),
                request(control.getSourceInquiryCode(), application.getNationalCode()),
                new InquirySubmissionModels.Callback(
                        InquirySubmissionModels.SPRING_BEAN,
                        OriginationControlCompletionHandler.KEY,
                        control.getId().toString()
                )
        )).requestId();
        control.inquirySubmitted(requestId);
    }

    private static Object request(ir.jetvam.common.inquiry.InquiryType type, String nationalCode) {
        return switch (type) {
            case CIVIL_REGISTRATION -> new InquiryRequests.CivilRegistration(nationalCode);
            case MILITARY_STATUS -> new InquiryRequests.MilitaryStatus(nationalCode);
            case BANK_ACCOUNT_STATUS -> new InquiryRequests.BankAccountStatus(nationalCode);
            case BANKING_FACILITIES -> new InquiryRequests.BankingFacilities(nationalCode);
            case BAD_CHEQUE -> new InquiryRequests.BadCheque(nationalCode);
            case CREDIT_RATING -> new InquiryRequests.CreditRating(nationalCode);
            default -> throw new IllegalArgumentException("Unsupported application inquiry: " + type);
        };
    }
}
