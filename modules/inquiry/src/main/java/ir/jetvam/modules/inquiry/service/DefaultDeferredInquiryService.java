package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.inquiry.provider.CreditRatingProtocol;
import ir.jetvam.modules.integration.routing.ProviderExecution;
import ir.jetvam.modules.integration.routing.ProviderRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Executes synchronous inquiries as background work and supports submit/poll credit-rating providers.
 * It returns normalized string facts so workflow persistence remains provider independent.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultDeferredInquiryService implements DeferredInquiryService {

    private final InquiryService inquiryService;
    private final ProviderRouter providerRouter;

    @Override
    public DeferredInquiryModels.Result execute(DeferredInquiryModels.Command command) {
        Preconditions.requireNonNull(command, "command");
        String nationalCode = IranianIdentifiers.normalizeNationalCode(command.nationalCode());
        Preconditions.require(IranianIdentifiers.isValidNationalCode(nationalCode), "nationalCode is invalid");
        return switch (command.inquiryCode()) {
            case InquiryCapabilities.CREDIT_RATING -> creditRating(command, nationalCode);
            case InquiryCapabilities.BAD_CHEQUE -> badCheque(nationalCode);
            case InquiryCapabilities.CIVIL_REGISTRATION -> civilRegistration(nationalCode);
            case InquiryCapabilities.MILITARY_STATUS -> militaryStatus(nationalCode);
            case InquiryCapabilities.BANK_ACCOUNT_STATUS -> bankAccountStatus(nationalCode);
            case InquiryCapabilities.BANKING_FACILITIES -> bankingFacilities(nationalCode);
            default -> throw new IllegalArgumentException("Unsupported deferred inquiry: " + command.inquiryCode());
        };
    }

    private DeferredInquiryModels.Result creditRating(
            DeferredInquiryModels.Command command,
            String nationalCode
    ) {
        CreditRatingProtocol.Progress progress;
        String providerCode;
        if (command.polling()) {
            providerCode = Preconditions.requireText(command.providerCode(), "providerCode");
            progress = providerRouter.executeOnProvider(
                    InquiryCapabilities.CREDIT_RATING_POLL,
                    providerCode,
                    new CreditRatingProtocol.Poll(command.externalTrackingCode()),
                    CreditRatingProtocol.Progress.class
            );
        } else {
            ProviderExecution<CreditRatingProtocol.Progress> execution = providerRouter.executeWithProvider(
                    InquiryCapabilities.CREDIT_RATING_SUBMIT,
                    new CreditRatingProtocol.Submit(nationalCode),
                    CreditRatingProtocol.Progress.class,
                    true
            );
            providerCode = execution.providerCode();
            progress = execution.result();
        }
        if (progress.status() == DeferredInquiryStatus.PENDING) {
            return DeferredInquiryModels.Result.pending(
                    providerCode,
                    Preconditions.requireText(progress.trackingCode(), "provider.trackingCode"),
                    Math.max(30, progress.retryAfterSeconds())
            );
        }
        if (progress.status() == DeferredInquiryStatus.REJECTED) {
            return new DeferredInquiryModels.Result(
                    DeferredInquiryStatus.REJECTED, providerCode, progress.trackingCode(), 0, Map.of(),
                    progress.rejectionCode(), progress.rejectionMessage()
            );
        }
        Preconditions.requireNonNull(progress.rank(), "provider.rank");
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("ratingCode", progress.ratingCode());
        facts.put("rank", progress.rank().toString());
        if (progress.score() != null) {
            facts.put("score", progress.score().toPlainString());
        }
        return DeferredInquiryModels.Result.completed(providerCode, progress.trackingCode(), facts);
    }

    private DeferredInquiryModels.Result badCheque(String nationalCode) {
        InquiryResults.BadCheque result = inquiryService.findBadCheques(new InquiryRequests.BadCheque(nationalCode));
        return completed(result.trackingId(), Map.of(
                "unsettledCount", Integer.toString(result.unsettledCount()),
                "unsettledAmount", result.totalAmount().toPlainString()
        ));
    }

    private DeferredInquiryModels.Result civilRegistration(String nationalCode) {
        InquiryResults.CivilRegistration result = inquiryService.findCivilRegistration(
                new InquiryRequests.CivilRegistration(nationalCode)
        );
        return completed(result.trackingId(), Map.of(
                "identityValid", Boolean.toString(result.identityValid()),
                "alive", Boolean.toString(result.alive())
        ));
    }

    private DeferredInquiryModels.Result militaryStatus(String nationalCode) {
        InquiryResults.MilitaryStatus result = inquiryService.findMilitaryStatus(
                new InquiryRequests.MilitaryStatus(nationalCode)
        );
        return completed(result.trackingId(), Map.of(
                "statusCode", result.statusCode(),
                "eligible", Boolean.toString(result.eligible())
        ));
    }

    private DeferredInquiryModels.Result bankAccountStatus(String nationalCode) {
        InquiryResults.BankAccountStatus result = inquiryService.findBankAccountStatus(
                new InquiryRequests.BankAccountStatus(nationalCode)
        );
        return completed(result.trackingId(), Map.of(
                "statusCode", result.statusCode(),
                "active", Boolean.toString(result.active())
        ));
    }

    private DeferredInquiryModels.Result bankingFacilities(String nationalCode) {
        InquiryResults.BankingFacilities result = inquiryService.findBankingFacilities(
                new InquiryRequests.BankingFacilities(nationalCode)
        );
        return completed(result.trackingId(), Map.of(
                "directCount", Integer.toString(result.directFacilityCount()),
                "indirectCount", Integer.toString(result.indirectFacilityCount()),
                "hasOverdueDebt", Boolean.toString(result.hasOverdueDebt()),
                "overdueAmount", result.overdueAmount().toPlainString()
        ));
    }

    private static DeferredInquiryModels.Result completed(String trackingId, Map<String, String> facts) {
        return DeferredInquiryModels.Result.completed(null, trackingId, facts);
    }
}
