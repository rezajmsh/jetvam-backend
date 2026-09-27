package ir.jetvam.modules.inquiry.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.integration.routing.ProviderExecution;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    private final RoutingInquiryService providerService;
    private final InquiryExecutionStore executionStore;
    private final ObjectMapper objectMapper;

    @Override
    public InquiryResults.MobileOwnership verifyMobileOwnership(InquiryRequests.MobileOwnership request) {
        Preconditions.requireNonNull(request, "request");
        String mobile = IranianIdentifiers.normalizeMobileNumber(request.mobile());
        String nationalCode = normalizedNationalCode(request.nationalCode());
        Preconditions.require(IranianIdentifiers.isValidMobileNumber(mobile), "mobile is invalid");
        InquiryRequests.MobileOwnership normalized = new InquiryRequests.MobileOwnership(mobile, nationalCode);
        return execute(
                InquiryCapabilities.MOBILE_OWNERSHIP,
                nationalCode,
                nationalCode + ":" + mobile,
                normalized,
                InquiryResults.MobileOwnership.class,
                () -> providerService.verifyMobileOwnership(normalized),
                InquiryResults.MobileOwnership::trackingId
        );
    }

    @Override
    public InquiryResults.CivilRegistration findCivilRegistration(InquiryRequests.CivilRegistration request) {
        String nationalCode = normalizedNationalCode(request == null ? null : request.nationalCode());
        InquiryRequests.CivilRegistration normalized = new InquiryRequests.CivilRegistration(nationalCode);
        return executeNationalCode(
                InquiryCapabilities.CIVIL_REGISTRATION, nationalCode, normalized,
                InquiryResults.CivilRegistration.class,
                () -> providerService.findCivilRegistration(normalized),
                InquiryResults.CivilRegistration::trackingId
        );
    }

    @Override
    public InquiryResults.MilitaryStatus findMilitaryStatus(InquiryRequests.MilitaryStatus request) {
        String nationalCode = normalizedNationalCode(request == null ? null : request.nationalCode());
        InquiryRequests.MilitaryStatus normalized = new InquiryRequests.MilitaryStatus(nationalCode);
        return executeNationalCode(
                InquiryCapabilities.MILITARY_STATUS, nationalCode, normalized,
                InquiryResults.MilitaryStatus.class,
                () -> providerService.findMilitaryStatus(normalized),
                InquiryResults.MilitaryStatus::trackingId
        );
    }

    @Override
    public InquiryResults.BankAccountStatus findBankAccountStatus(InquiryRequests.BankAccountStatus request) {
        String nationalCode = normalizedNationalCode(request == null ? null : request.nationalCode());
        InquiryRequests.BankAccountStatus normalized = new InquiryRequests.BankAccountStatus(nationalCode);
        return executeNationalCode(
                InquiryCapabilities.BANK_ACCOUNT_STATUS, nationalCode, normalized,
                InquiryResults.BankAccountStatus.class,
                () -> providerService.findBankAccountStatus(normalized),
                InquiryResults.BankAccountStatus::trackingId
        );
    }

    @Override
    public InquiryResults.BankingFacilities findBankingFacilities(InquiryRequests.BankingFacilities request) {
        String nationalCode = normalizedNationalCode(request == null ? null : request.nationalCode());
        InquiryRequests.BankingFacilities normalized = new InquiryRequests.BankingFacilities(nationalCode);
        return executeNationalCode(
                InquiryCapabilities.BANKING_FACILITIES, nationalCode, normalized,
                InquiryResults.BankingFacilities.class,
                () -> providerService.findBankingFacilities(normalized),
                InquiryResults.BankingFacilities::trackingId
        );
    }

    @Override
    public InquiryResults.BadCheque findBadCheques(InquiryRequests.BadCheque request) {
        String nationalCode = normalizedNationalCode(request == null ? null : request.nationalCode());
        InquiryRequests.BadCheque normalized = new InquiryRequests.BadCheque(nationalCode);
        return executeNationalCode(
                InquiryCapabilities.BAD_CHEQUE, nationalCode, normalized,
                InquiryResults.BadCheque.class,
                () -> providerService.findBadCheques(normalized),
                InquiryResults.BadCheque::trackingId
        );
    }

    @Override
    public InquiryResults.CreditRating findCreditRating(InquiryRequests.CreditRating request) {
        String nationalCode = normalizedNationalCode(request == null ? null : request.nationalCode());
        InquiryRequests.CreditRating normalized = new InquiryRequests.CreditRating(nationalCode);
        return executeNationalCode(
                InquiryCapabilities.CREDIT_RATING, nationalCode, normalized,
                InquiryResults.CreditRating.class,
                () -> providerService.findCreditRating(normalized),
                InquiryResults.CreditRating::trackingId
        );
    }

    private <C, R> R executeNationalCode(
            String inquiryCode,
            String nationalCode,
            C command,
            Class<R> resultType,
            ProviderCall<R> providerCall,
            Function<R, String> trackingId
    ) {
        return execute(
                inquiryCode, nationalCode, nationalCode, command, resultType, providerCall, trackingId
        );
    }

    private <C, R> R execute(
            String inquiryCode,
            String nationalCode,
            String subjectKey,
            C command,
            Class<R> resultType,
            ProviderCall<R> providerCall,
            Function<R, String> trackingId
    ) {
        String requestJson = write(command);
        InquiryExecutionStore.Preparation preparation = executionStore.prepare(
                inquiryCode, nationalCode, subjectKey, requestJson
        );
        if (preparation.cacheHit()) {
            return read(preparation.cachedResultJson(), resultType);
        }
        try {
            ProviderExecution<R> execution = providerCall.execute();
            R result = execution.result();
            executionStore.complete(
                    preparation.requestId(), execution.providerCode(), trackingId.apply(result),
                    write(result), preparation.validity()
            );
            return result;
        } catch (RuntimeException failure) {
            try {
                executionStore.fail(preparation.requestId(), failure);
            } catch (RuntimeException persistenceFailure) {
                failure.addSuppressed(persistenceFailure);
            }
            throw failure;
        }
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Unable to persist inquiry payload", exception);
        }
    }

    private <R> R read(String json, Class<R> resultType) {
        try {
            return objectMapper.readValue(json, resultType);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Unable to read cached inquiry result", exception);
        }
    }

    private static String normalizedNationalCode(String value) {
        String nationalCode = IranianIdentifiers.normalizeNationalCode(value);
        Preconditions.require(IranianIdentifiers.isValidNationalCode(nationalCode), "nationalCode is invalid");
        return nationalCode;
    }

    @FunctionalInterface
    private interface ProviderCall<R> {
        ProviderExecution<R> execute();
    }
}
