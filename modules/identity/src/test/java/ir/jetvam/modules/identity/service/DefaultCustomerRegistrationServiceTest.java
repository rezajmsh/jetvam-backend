package ir.jetvam.modules.identity.service;

import ir.jetvam.modules.identity.IdentityOtpPurposes;
import ir.jetvam.modules.identity.model.CustomerOnboardingStatus;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.inquiry.service.InquiryService;
import ir.jetvam.modules.otp.service.OtpChallengeService;
import ir.jetvam.modules.otp.service.OtpVerificationData;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies that customer registration places Shahkar outside all local transaction phases.
 * OTP consumption and account provisioning are delegated to the completion transaction.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
class DefaultCustomerRegistrationServiceTest {

    @Test
    void callsShahkarBetweenOtpValidationAndTransactionalCompletion() {
        UUID challengeId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OtpChallengeService otpService = mock(OtpChallengeService.class);
        InquiryService inquiryService = mock(InquiryService.class);
        CustomerRegistrationTransactionService transactions = mock(CustomerRegistrationTransactionService.class);
        OtpVerificationData verified = new OtpVerificationData("09121234567", "1234567890");
        InquiryResults.MobileOwnership ownership = new InquiryResults.MobileOwnership(true, "tracking-1");
        CustomerRegistrationResult expected = new CustomerRegistrationResult(
                userId,
                partyId,
                CustomerOnboardingStatus.IDENTITY_VERIFIED
        );
        VerifyCustomerRegistrationCommand command = new VerifyCustomerRegistrationCommand(challengeId, "123456");

        when(otpService.verify(challengeId, "123456", IdentityOtpPurposes.CUSTOMER_REGISTRATION))
                .thenReturn(verified);
        when(inquiryService.verifyMobileOwnership(new InquiryRequests.MobileOwnership(
                verified.mobile(), verified.nationalCode()
        ))).thenReturn(ownership);
        when(transactions.complete(command, ownership))
                .thenReturn(CustomerRegistrationCompletion.accepted(expected));

        var service = new DefaultCustomerRegistrationService(
                otpService,
                inquiryService,
                transactions
        );
        CustomerRegistrationResult result = service.verifyOtp(command);

        assertThat(result).isEqualTo(expected);
        InOrder order = inOrder(otpService, transactions, inquiryService);
        order.verify(otpService).verify(
                challengeId,
                "123456",
                IdentityOtpPurposes.CUSTOMER_REGISTRATION
        );
        order.verify(transactions).assertRegistrationAvailable(verified.mobile(), verified.nationalCode());
        order.verify(inquiryService).verifyMobileOwnership(new InquiryRequests.MobileOwnership(
                verified.mobile(), verified.nationalCode()
        ));
        order.verify(transactions).complete(command, ownership);
    }
}
