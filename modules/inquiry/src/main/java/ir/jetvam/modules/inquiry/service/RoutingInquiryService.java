package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.exception.IntegrationException;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.integration.routing.ProviderExecution;
import ir.jetvam.modules.integration.routing.ProviderRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Executes already-normalized inquiry commands through Integration's runtime provider router.
 * Persistence and cache policy stay in the public inquiry service, outside provider adapters.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class RoutingInquiryService {

    private final ProviderRouter providerRouter;

    public ProviderExecution<InquiryResults.MobileOwnership> verifyMobileOwnership(
            InquiryRequests.MobileOwnership request
    ) {
        return execute(
                InquiryType.MOBILE_OWNERSHIP.code(),
                request,
                InquiryResults.MobileOwnership.class,
                "mobile-ownership"
        );
    }

    public ProviderExecution<InquiryResults.BadCheque> findBadCheques(InquiryRequests.BadCheque request) {
        return execute(
                InquiryType.BAD_CHEQUE.code(),
                request,
                InquiryResults.BadCheque.class,
                "bad-cheque"
        );
    }

    public ProviderExecution<InquiryResults.CivilRegistration> findCivilRegistration(
            InquiryRequests.CivilRegistration request
    ) {
        return execute(
                InquiryType.CIVIL_REGISTRATION.code(),
                request,
                InquiryResults.CivilRegistration.class,
                "civil-registration"
        );
    }

    public ProviderExecution<InquiryResults.MilitaryStatus> findMilitaryStatus(
            InquiryRequests.MilitaryStatus request
    ) {
        return execute(
                InquiryType.MILITARY_STATUS.code(),
                request,
                InquiryResults.MilitaryStatus.class,
                "military-status"
        );
    }

    public ProviderExecution<InquiryResults.BankAccountStatus> findBankAccountStatus(
            InquiryRequests.BankAccountStatus request
    ) {
        return execute(
                InquiryType.BANK_ACCOUNT_STATUS.code(),
                request,
                InquiryResults.BankAccountStatus.class,
                "bank-account-status"
        );
    }

    public ProviderExecution<InquiryResults.BankingFacilities> findBankingFacilities(
            InquiryRequests.BankingFacilities request
    ) {
        return execute(
                InquiryType.BANKING_FACILITIES.code(),
                request,
                InquiryResults.BankingFacilities.class,
                "banking-facilities"
        );
    }

    public ProviderExecution<InquiryResults.CreditRating> findCreditRating(InquiryRequests.CreditRating request) {
        return execute(
                InquiryType.CREDIT_RATING.code(),
                request,
                InquiryResults.CreditRating.class,
                "credit-rating"
        );
    }

    private <C, R> ProviderExecution<R> execute(
            String capability,
            C request,
            Class<R> resultType,
            String operation
    ) {
        try {
            return providerRouter.executeWithProvider(capability, request, resultType, true);
        } catch (RuntimeException exception) {
            throw new IntegrationException("inquiry", operation, exception);
        }
    }

}
