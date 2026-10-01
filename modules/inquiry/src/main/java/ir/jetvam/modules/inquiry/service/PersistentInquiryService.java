package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.exception.IntegrationException;
import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.common.inquiry.InquiryType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;

/**
 * Implements the public inquiry boundary with durable execution and validity-aware reuse.
 * Provider I/O occurs only after the preparation transaction commits and before completion persists.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
@Service
@RequiredArgsConstructor
public class PersistentInquiryService implements InquiryService {

    private final InquiryRequestService inquiryRequestService;

    @Override
    public InquiryResults.MobileOwnership verifyMobileOwnership(InquiryRequests.MobileOwnership request) {
        Preconditions.requireNonNull(request, "request");
        String mobile = IranianIdentifiers.normalizeMobileNumber(request.mobile());
        String nationalCode = normalizedNationalCode(request.nationalCode());
        Preconditions.require(IranianIdentifiers.isValidMobileNumber(mobile), "mobile is invalid");
        InquiryRequests.MobileOwnership normalized = new InquiryRequests.MobileOwnership(mobile, nationalCode);
        return execute(
                InquiryType.MOBILE_OWNERSHIP,
                nationalCode,
                nationalCode + ":" + mobile,
                normalized,
                PersistentInquiryService::mobileOwnership
        );
    }

    @Override
    public InquiryResults.CivilRegistration findCivilRegistration(InquiryRequests.CivilRegistration request) {
        String nationalCode = normalizedNationalCode(request == null ? null : request.nationalCode());
        InquiryRequests.CivilRegistration normalized = new InquiryRequests.CivilRegistration(nationalCode);
        return executeNationalCode(
                InquiryType.CIVIL_REGISTRATION, nationalCode, normalized,
                PersistentInquiryService::civilRegistration
        );
    }

    @Override
    public InquiryResults.MilitaryStatus findMilitaryStatus(InquiryRequests.MilitaryStatus request) {
        String nationalCode = normalizedNationalCode(request == null ? null : request.nationalCode());
        InquiryRequests.MilitaryStatus normalized = new InquiryRequests.MilitaryStatus(nationalCode);
        return executeNationalCode(
                InquiryType.MILITARY_STATUS, nationalCode, normalized,
                PersistentInquiryService::militaryStatus
        );
    }

    @Override
    public InquiryResults.BankAccountStatus findBankAccountStatus(InquiryRequests.BankAccountStatus request) {
        String nationalCode = normalizedNationalCode(request == null ? null : request.nationalCode());
        InquiryRequests.BankAccountStatus normalized = new InquiryRequests.BankAccountStatus(nationalCode);
        return executeNationalCode(
                InquiryType.BANK_ACCOUNT_STATUS, nationalCode, normalized,
                PersistentInquiryService::bankAccountStatus
        );
    }

    @Override
    public InquiryResults.BankingFacilities findBankingFacilities(InquiryRequests.BankingFacilities request) {
        String nationalCode = normalizedNationalCode(request == null ? null : request.nationalCode());
        InquiryRequests.BankingFacilities normalized = new InquiryRequests.BankingFacilities(nationalCode);
        return executeNationalCode(
                InquiryType.BANKING_FACILITIES, nationalCode, normalized,
                PersistentInquiryService::bankingFacilities
        );
    }

    @Override
    public InquiryResults.BadCheque findBadCheques(InquiryRequests.BadCheque request) {
        String nationalCode = normalizedNationalCode(request == null ? null : request.nationalCode());
        InquiryRequests.BadCheque normalized = new InquiryRequests.BadCheque(nationalCode);
        return executeNationalCode(
                InquiryType.BAD_CHEQUE, nationalCode, normalized,
                PersistentInquiryService::badCheque
        );
    }

    @Override
    public InquiryResults.CreditRating findCreditRating(InquiryRequests.CreditRating request) {
        String nationalCode = normalizedNationalCode(request == null ? null : request.nationalCode());
        InquiryRequests.CreditRating normalized = new InquiryRequests.CreditRating(nationalCode);
        return executeNationalCode(
                InquiryType.CREDIT_RATING, nationalCode, normalized,
                PersistentInquiryService::creditRating
        );
    }

    private <C, R> R executeNationalCode(
            InquiryType inquiryCode,
            String nationalCode,
            C command,
            Function<InquirySubmissionModels.Result, R> resultMapper
    ) {
        return execute(
                inquiryCode, nationalCode, nationalCode, command, resultMapper
        );
    }

    private <C, R> R execute(
            InquiryType inquiryCode,
            String nationalCode,
            String subjectKey,
            C command,
            Function<InquirySubmissionModels.Result, R> resultMapper
    ) {
        InquirySubmissionModels.Result result = inquiryRequestService.submit(
                InquirySubmissionModels.Command.synchronous(
                        inquiryCode, nationalCode, subjectKey, command
                )
        );
        if (result.status() != ir.jetvam.modules.inquiry.model.InquiryStatus.COMPLETED) {
            throw new IntegrationException(
                    result.providerCode() == null ? "inquiry" : result.providerCode(),
                    inquiryCode.code(),
                    new IllegalStateException(result.message() == null ? result.status().name() : result.message())
            );
        }
        return resultMapper.apply(result);
    }

    private static String normalizedNationalCode(String value) {
        String nationalCode = IranianIdentifiers.normalizeNationalCode(value);
        Preconditions.require(IranianIdentifiers.isValidNationalCode(nationalCode), "nationalCode is invalid");
        return nationalCode;
    }

    private static InquiryResults.MobileOwnership mobileOwnership(InquirySubmissionModels.Result execution) {
        return new InquiryResults.MobileOwnership(
                Boolean.parseBoolean(required(execution.facts(), "matched")), trackingId(execution)
        );
    }

    private static InquiryResults.CivilRegistration civilRegistration(InquirySubmissionModels.Result execution) {
        return new InquiryResults.CivilRegistration(
                Boolean.parseBoolean(required(execution.facts(), "identityValid")),
                Boolean.parseBoolean(required(execution.facts(), "alive")), trackingId(execution)
        );
    }

    private static InquiryResults.MilitaryStatus militaryStatus(InquirySubmissionModels.Result execution) {
        return new InquiryResults.MilitaryStatus(
                required(execution.facts(), "statusCode"),
                Boolean.parseBoolean(required(execution.facts(), "eligible")), trackingId(execution)
        );
    }

    private static InquiryResults.BankAccountStatus bankAccountStatus(InquirySubmissionModels.Result execution) {
        return new InquiryResults.BankAccountStatus(
                required(execution.facts(), "statusCode"),
                Boolean.parseBoolean(required(execution.facts(), "active")), trackingId(execution)
        );
    }

    private static InquiryResults.BankingFacilities bankingFacilities(InquirySubmissionModels.Result execution) {
        Map<String, String> facts = execution.facts();
        return new InquiryResults.BankingFacilities(
                Integer.parseInt(required(facts, "directFacilityCount")),
                Integer.parseInt(required(facts, "indirectFacilityCount")),
                Boolean.parseBoolean(required(facts, "hasOverdueDebt")),
                new BigDecimal(required(facts, "overdueAmount")), trackingId(execution)
        );
    }

    private static InquiryResults.BadCheque badCheque(InquirySubmissionModels.Result execution) {
        return new InquiryResults.BadCheque(
                Integer.parseInt(required(execution.facts(), "unsettledCount")),
                new BigDecimal(required(execution.facts(), "totalAmount")), trackingId(execution)
        );
    }

    private static InquiryResults.CreditRating creditRating(InquirySubmissionModels.Result execution) {
        String score = execution.facts().get("score");
        return new InquiryResults.CreditRating(
                required(execution.facts(), "ratingCode"),
                Integer.parseInt(required(execution.facts(), "rank")),
                score == null ? null : new BigDecimal(score), trackingId(execution)
        );
    }

    private static String trackingId(InquirySubmissionModels.Result execution) {
        return execution.facts().get("trackingId");
    }

    private static String required(Map<String, String> facts, String key) {
        return Preconditions.requireText(facts.get(key), "provider." + key);
    }
}
