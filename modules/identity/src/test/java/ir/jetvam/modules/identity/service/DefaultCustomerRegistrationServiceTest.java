package ir.jetvam.modules.identity.service;

import ir.jetvam.common.time.ClockTimeProvider;
import ir.jetvam.modules.identity.IdentityOtpPurposes;
import ir.jetvam.modules.identity.model.CustomerOnboardingStatus;
import ir.jetvam.modules.identity.repository.CustomerProfileRepository;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import ir.jetvam.modules.integration.shahkar.ShahkarProvider;
import ir.jetvam.modules.integration.shahkar.ShahkarVerification;
import ir.jetvam.modules.otp.service.OtpChallengeService;
import ir.jetvam.modules.otp.service.OtpVerificationData;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
        ShahkarProvider shahkarProvider = mock(ShahkarProvider.class);
        CustomerRegistrationTransactionService transactions = mock(CustomerRegistrationTransactionService.class);
        OtpVerificationData verified = new OtpVerificationData("09121234567", "1234567890");
        ShahkarVerification shahkar = new ShahkarVerification(true, "tracking-1");
        CustomerRegistrationResult expected = new CustomerRegistrationResult(
                userId,
                partyId,
                CustomerOnboardingStatus.IDENTITY_VERIFIED
        );
        VerifyCustomerRegistrationCommand command = new VerifyCustomerRegistrationCommand(challengeId, "123456");

        when(otpService.verify(challengeId, "123456", IdentityOtpPurposes.CUSTOMER_REGISTRATION))
                .thenReturn(verified);
        when(shahkarProvider.verify(verified.mobile(), verified.nationalCode())).thenReturn(shahkar);
        when(transactions.complete(command, shahkar))
                .thenReturn(CustomerRegistrationCompletion.accepted(expected));

        var service = new DefaultCustomerRegistrationService(
                otpService,
                shahkarProvider,
                transactions,
                mock(IndividualPartyRepository.class),
                mock(UserAccountRepository.class),
                mock(CustomerProfileRepository.class),
                new ClockTimeProvider(Clock.fixed(Instant.parse("2026-09-22T03:30:00Z"), ZoneOffset.UTC))
        );
        CustomerRegistrationResult result = service.verifyOtp(command);

        assertThat(result).isEqualTo(expected);
        InOrder order = inOrder(otpService, transactions, shahkarProvider);
        order.verify(otpService).verify(
                challengeId,
                "123456",
                IdentityOtpPurposes.CUSTOMER_REGISTRATION
        );
        order.verify(transactions).assertRegistrationAvailable(verified.mobile(), verified.nationalCode());
        order.verify(shahkarProvider).verify(verified.mobile(), verified.nationalCode());
        order.verify(transactions).complete(command, shahkar);
    }
}
