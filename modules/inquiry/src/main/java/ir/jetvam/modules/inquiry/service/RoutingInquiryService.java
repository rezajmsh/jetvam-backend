package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.exception.IntegrationException;
import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.integration.routing.ProviderRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Function;

/**
 * Executes canonical inquiries through Integration's runtime provider router.
 * Input normalization occurs before network I/O and read-only inquiries are safe for failover.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class RoutingInquiryService implements InquiryService {

    private final ProviderRouter providerRouter;

    @Override
    public InquiryResults.MobileOwnership verifyMobileOwnership(InquiryRequests.MobileOwnership request) {
        Preconditions.requireNonNull(request, "request");
        String mobile = IranianIdentifiers.normalizeMobileNumber(request.mobile());
        String nationalCode = IranianIdentifiers.normalizeNationalCode(request.nationalCode());
        Preconditions.require(IranianIdentifiers.isValidMobileNumber(mobile), "mobile is invalid");
        requireNationalCode(nationalCode);
        return execute(
                InquiryCapabilities.MOBILE_OWNERSHIP,
                new InquiryRequests.MobileOwnership(mobile, nationalCode),
                InquiryResults.MobileOwnership.class,
                "mobile-ownership"
        );
    }

    @Override
    public InquiryResults.BadCheque findBadCheques(InquiryRequests.BadCheque request) {
        String nationalCode = normalizedNationalCode(request, InquiryRequests.BadCheque::nationalCode);
        return execute(
                InquiryCapabilities.BAD_CHEQUE,
                new InquiryRequests.BadCheque(nationalCode),
                InquiryResults.BadCheque.class,
                "bad-cheque"
        );
    }

    @Override
    public InquiryResults.CivilRegistration findCivilRegistration(InquiryRequests.CivilRegistration request) {
        String nationalCode = normalizedNationalCode(request, InquiryRequests.CivilRegistration::nationalCode);
        return execute(
                InquiryCapabilities.CIVIL_REGISTRATION,
                new InquiryRequests.CivilRegistration(nationalCode),
                InquiryResults.CivilRegistration.class,
                "civil-registration"
        );
    }

    @Override
    public InquiryResults.MilitaryStatus findMilitaryStatus(InquiryRequests.MilitaryStatus request) {
        String nationalCode = normalizedNationalCode(request, InquiryRequests.MilitaryStatus::nationalCode);
        return execute(
                InquiryCapabilities.MILITARY_STATUS,
                new InquiryRequests.MilitaryStatus(nationalCode),
                InquiryResults.MilitaryStatus.class,
                "military-status"
        );
    }

    @Override
    public InquiryResults.BankAccountStatus findBankAccountStatus(InquiryRequests.BankAccountStatus request) {
        String nationalCode = normalizedNationalCode(request, InquiryRequests.BankAccountStatus::nationalCode);
        return execute(
                InquiryCapabilities.BANK_ACCOUNT_STATUS,
                new InquiryRequests.BankAccountStatus(nationalCode),
                InquiryResults.BankAccountStatus.class,
                "bank-account-status"
        );
    }

    @Override
    public InquiryResults.BankingFacilities findBankingFacilities(InquiryRequests.BankingFacilities request) {
        String nationalCode = normalizedNationalCode(request, InquiryRequests.BankingFacilities::nationalCode);
        return execute(
                InquiryCapabilities.BANKING_FACILITIES,
                new InquiryRequests.BankingFacilities(nationalCode),
                InquiryResults.BankingFacilities.class,
                "banking-facilities"
        );
    }

    @Override
    public InquiryResults.CreditRating findCreditRating(InquiryRequests.CreditRating request) {
        String nationalCode = normalizedNationalCode(request, InquiryRequests.CreditRating::nationalCode);
        return execute(
                InquiryCapabilities.CREDIT_RATING,
                new InquiryRequests.CreditRating(nationalCode),
                InquiryResults.CreditRating.class,
                "credit-rating"
        );
    }

    private <C, R> R execute(String capability, C request, Class<R> resultType, String operation) {
        try {
            return providerRouter.execute(capability, request, resultType, true);
        } catch (RuntimeException exception) {
            throw new IntegrationException("inquiry", operation, exception);
        }
    }

    private static void requireNationalCode(String nationalCode) {
        Preconditions.require(IranianIdentifiers.isValidNationalCode(nationalCode), "nationalCode is invalid");
    }

    private static <T> String normalizedNationalCode(T request, Function<T, String> nationalCodeExtractor) {
        Preconditions.requireNonNull(request, "request");
        String nationalCode = IranianIdentifiers.normalizeNationalCode(nationalCodeExtractor.apply(request));
        requireNationalCode(nationalCode);
        return nationalCode;
    }
}
