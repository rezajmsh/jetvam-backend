package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.IdentityOtpPurposes;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.inquiry.service.InquiryService;
import ir.jetvam.modules.otp.service.OtpChallengeService;
import ir.jetvam.modules.otp.service.OtpChallengeView;
import ir.jetvam.modules.otp.service.OtpVerificationData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orchestrates OTP, Shahkar and initial customer account provisioning.
 * Existing parties are reused only when both canonical identifiers agree.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultCustomerRegistrationService implements CustomerRegistrationService {

    private final OtpChallengeService otpChallengeService;
    private final InquiryService inquiryService;
    private final CustomerRegistrationTransactionService registrationTransactions;

    @Override
    public OtpChallengeView requestOtp(StartCustomerRegistrationCommand command) {
        Preconditions.requireNonNull(command, "command");
        String mobile = IranianIdentifiers.normalizeMobileNumber(command.mobile());
        String nationalCode = IranianIdentifiers.normalizeNationalCode(command.nationalCode());
        Preconditions.require(IranianIdentifiers.isValidMobileNumber(mobile), "mobile is invalid");
        Preconditions.require(IranianIdentifiers.isValidNationalCode(nationalCode), "nationalCode is invalid");
        return otpChallengeService.issue(mobile, nationalCode, IdentityOtpPurposes.CUSTOMER_REGISTRATION);
    }

    @Override
    public CustomerRegistrationResult verifyOtp(VerifyCustomerRegistrationCommand command) {
        Preconditions.requireNonNull(command, "command");
        OtpVerificationData verified = otpChallengeService.verify(
                command.challengeId(),
                command.otp(),
                IdentityOtpPurposes.CUSTOMER_REGISTRATION
        );
        registrationTransactions.assertRegistrationAvailable(verified.mobile(), verified.nationalCode());
        InquiryResults.MobileOwnership ownership = inquiryService.verifyMobileOwnership(
                new InquiryRequests.MobileOwnership(verified.mobile(), verified.nationalCode())
        );
        CustomerRegistrationCompletion completion = registrationTransactions.complete(command, ownership);
        if (!completion.matched()) {
            throw new ValidationException("Mobile ownership could not be verified");
        }
        return completion.registration();
    }

}
