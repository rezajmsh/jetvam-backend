package ir.jetvam.modules.identity.service;

import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.security.UserCategory;
import ir.jetvam.modules.identity.IdentityErrorCode;
import ir.jetvam.modules.identity.IdentityOtpPurposes;
import ir.jetvam.modules.identity.exception.CustomerAccountUnavailableException;
import ir.jetvam.modules.identity.exception.CustomerRegistrationRequiredException;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import ir.jetvam.modules.otp.service.OtpChallengeService;
import ir.jetvam.modules.otp.service.OtpVerificationData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifies that customer OTP login distinguishes missing registration from invalid OTP.
 * Unknown customers are rejected before notification and rechecked after OTP consumption.
 *
 * @author reza jamshidi
 * @since 9/27/2026
 */
class DefaultCustomerAuthenticationServiceTest {

    private static final String MOBILE = "09121234567";

    private OtpChallengeService otpService;
    private IndividualPartyRepository individualRepository;
    private UserAccountRepository userRepository;
    private DefaultCustomerAuthenticationService service;

    @BeforeEach
    void setUp() {
        otpService = mock(OtpChallengeService.class);
        individualRepository = mock(IndividualPartyRepository.class);
        userRepository = mock(UserAccountRepository.class);
        service = new DefaultCustomerAuthenticationService(
                otpService,
                individualRepository,
                userRepository,
                mock(TimeProvider.class)
        );
    }

    @Test
    void rejectsUnknownCustomerBeforeIssuingLoginOtp() {
        when(individualRepository.findByMobile(MOBILE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestOtp(MOBILE))
                .isInstanceOfSatisfying(CustomerRegistrationRequiredException.class, exception -> {
                    assertThat(exception.code())
                            .isEqualTo(IdentityErrorCode.CUSTOMER_REGISTRATION_REQUIRED.code());
                    assertThat(exception.getMessage())
                            .isEqualTo("Customer registration must be completed before signing in");
                });

        verifyNoInteractions(otpService);
    }

    @Test
    void reportsMissingCustomerAfterAValidOtpInsteadOfCallingItInvalid() {
        UUID challengeId = UUID.randomUUID();
        when(otpService.consume(challengeId, "123456", IdentityOtpPurposes.CUSTOMER_LOGIN))
                .thenReturn(new OtpVerificationData(MOBILE, null));
        when(individualRepository.findByMobile(MOBILE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(challengeId, "123456"))
                .isInstanceOf(CustomerRegistrationRequiredException.class)
                .hasMessage("Customer registration must be completed before signing in");

        verify(otpService).consume(challengeId, "123456", IdentityOtpPurposes.CUSTOMER_LOGIN);
    }

    @Test
    void rejectsUnavailableCustomerBeforeIssuingLoginOtp() {
        UUID partyId = UUID.randomUUID();
        IndividualPartyEntity individual = mock(IndividualPartyEntity.class);
        UserAccountEntity account = mock(UserAccountEntity.class);
        when(individual.getId()).thenReturn(partyId);
        when(individualRepository.findByMobile(MOBILE)).thenReturn(Optional.of(individual));
        when(userRepository.findByParty_IdAndPrimaryAuthenticationMethod(partyId, AuthenticationMethod.OTP))
                .thenReturn(Optional.of(account));
        when(account.getCategories()).thenReturn(Set.of(UserCategory.CUSTOMER));
        when(account.getStatus()).thenReturn(UserAccountStatus.DISABLED);

        assertThatThrownBy(() -> service.requestOtp(MOBILE))
                .isInstanceOfSatisfying(CustomerAccountUnavailableException.class, exception ->
                        assertThat(exception.code())
                                .isEqualTo(IdentityErrorCode.CUSTOMER_ACCOUNT_UNAVAILABLE.code())
                );

        verifyNoInteractions(otpService);
    }
}
