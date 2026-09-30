package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.common.inquiry.InquiryType;
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

    private final RoutingInquiryService providerService;
    private final ProviderRouter providerRouter;

    @Override
    public DeferredInquiryModels.Result execute(DeferredInquiryModels.Command command) {
        Preconditions.requireNonNull(command, "command");
        String nationalCode = IranianIdentifiers.normalizeNationalCode(command.nationalCode());
        Preconditions.require(IranianIdentifiers.isValidNationalCode(nationalCode), "nationalCode is invalid");
        return switch (InquiryType.requireDeferred(command.inquiryCode())) {
            case CREDIT_RATING -> creditRating(command, nationalCode);
            case BAD_CHEQUE -> badCheque(nationalCode);
            case CIVIL_REGISTRATION -> civilRegistration(nationalCode);
            case MILITARY_STATUS -> militaryStatus(nationalCode);
            case BANK_ACCOUNT_STATUS -> bankAccountStatus(nationalCode);
            case BANKING_FACILITIES -> bankingFacilities(nationalCode);
            default -> throw new IllegalStateException("Unsupported deferred inquiry: " + command.inquiryCode());
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
                    InquiryType.CREDIT_RATING_POLL.code(),
                    providerCode,
                    new CreditRatingProtocol.Poll(command.externalTrackingCode()),
                    CreditRatingProtocol.Progress.class
            );
        } else {
            ProviderExecution<CreditRatingProtocol.Progress> execution = providerRouter.executeWithProvider(
                    InquiryType.CREDIT_RATING_SUBMIT.code(),
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
        if (progress.trackingCode() != null) {
            facts.put("trackingId", progress.trackingCode());
        }
        return DeferredInquiryModels.Result.completed(providerCode, progress.trackingCode(), facts);
    }

    private DeferredInquiryModels.Result badCheque(String nationalCode) {
        ProviderExecution<InquiryResults.BadCheque> execution = providerService.findBadCheques(
                new InquiryRequests.BadCheque(nationalCode)
        );
        InquiryResults.BadCheque result = execution.result();
        return completed(execution.providerCode(), result.trackingId(), factsWithTracking(Map.of(
                "unsettledCount", Integer.toString(result.unsettledCount()),
                "totalAmount", result.totalAmount().toPlainString()
        ), result.trackingId()));
    }

    private DeferredInquiryModels.Result civilRegistration(String nationalCode) {
        ProviderExecution<InquiryResults.CivilRegistration> execution = providerService.findCivilRegistration(
                new InquiryRequests.CivilRegistration(nationalCode)
        );
        InquiryResults.CivilRegistration result = execution.result();
        return completed(execution.providerCode(), result.trackingId(), factsWithTracking(Map.of(
                "identityValid", Boolean.toString(result.identityValid()),
                "alive", Boolean.toString(result.alive())
        ), result.trackingId()));
    }

    private DeferredInquiryModels.Result militaryStatus(String nationalCode) {
        ProviderExecution<InquiryResults.MilitaryStatus> execution = providerService.findMilitaryStatus(
                new InquiryRequests.MilitaryStatus(nationalCode)
        );
        InquiryResults.MilitaryStatus result = execution.result();
        return completed(execution.providerCode(), result.trackingId(), factsWithTracking(Map.of(
                "statusCode", result.statusCode(),
                "eligible", Boolean.toString(result.eligible())
        ), result.trackingId()));
    }

    private DeferredInquiryModels.Result bankAccountStatus(String nationalCode) {
        ProviderExecution<InquiryResults.BankAccountStatus> execution = providerService.findBankAccountStatus(
                new InquiryRequests.BankAccountStatus(nationalCode)
        );
        InquiryResults.BankAccountStatus result = execution.result();
        return completed(execution.providerCode(), result.trackingId(), factsWithTracking(Map.of(
                "statusCode", result.statusCode(),
                "active", Boolean.toString(result.active())
        ), result.trackingId()));
    }

    private DeferredInquiryModels.Result bankingFacilities(String nationalCode) {
        ProviderExecution<InquiryResults.BankingFacilities> execution = providerService.findBankingFacilities(
                new InquiryRequests.BankingFacilities(nationalCode)
        );
        InquiryResults.BankingFacilities result = execution.result();
        return completed(execution.providerCode(), result.trackingId(), factsWithTracking(Map.of(
                "directFacilityCount", Integer.toString(result.directFacilityCount()),
                "indirectFacilityCount", Integer.toString(result.indirectFacilityCount()),
                "hasOverdueDebt", Boolean.toString(result.hasOverdueDebt()),
                "overdueAmount", result.overdueAmount().toPlainString()
        ), result.trackingId()));
    }

    private static DeferredInquiryModels.Result completed(
            String providerCode,
            String trackingId,
            Map<String, String> facts
    ) {
        return DeferredInquiryModels.Result.completed(providerCode, trackingId, facts);
    }

    private static Map<String, String> factsWithTracking(Map<String, String> facts, String trackingId) {
        if (trackingId == null || trackingId.isBlank()) {
            return facts;
        }
        Map<String, String> result = new LinkedHashMap<>(facts);
        result.put("trackingId", trackingId);
        return result;
    }
}
