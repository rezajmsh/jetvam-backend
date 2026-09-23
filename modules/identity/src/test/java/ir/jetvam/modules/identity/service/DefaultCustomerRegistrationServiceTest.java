package ir.jetvam.modules.identity.service;

import ir.jetvam.common.time.ClockTimeProvider;
import ir.jetvam.modules.identity.IdentityRoles;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.model.CustomerOnboardingStatus;
import ir.jetvam.modules.identity.model.OtpPurpose;
import ir.jetvam.modules.identity.model.ShahkarStatus;
import ir.jetvam.modules.identity.persistence.CustomerProfileEntity;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.persistence.RoleEntity;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.repository.CustomerProfileRepository;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import ir.jetvam.modules.identity.repository.RoleRepository;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that successful OTP and Shahkar checks create a passwordless customer account.
 * Initial profile data remains incomplete until the authenticated customer supplies it.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
class DefaultCustomerRegistrationServiceTest {

    @Test
    void provisionsOtpAccountOnlyAfterShahkarMatch() {
        UUID challengeId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OtpChallengeService otpService = mock(OtpChallengeService.class);
        ShahkarProvider shahkarProvider = mock(ShahkarProvider.class);
        IndividualPartyRepository individualRepository = mock(IndividualPartyRepository.class);
        UserAccountRepository userRepository = mock(UserAccountRepository.class);
        RoleRepository roleRepository = mock(RoleRepository.class);
        CustomerProfileRepository profileRepository = mock(CustomerProfileRepository.class);
        RoleEntity customerRole = mock(RoleEntity.class);

        when(otpService.consume(challengeId, "123456", OtpPurpose.CUSTOMER_REGISTRATION))
                .thenReturn(new OtpVerificationData("09121234567", "1234567890"));
        when(shahkarProvider.verify("09121234567", "1234567890"))
                .thenReturn(new ShahkarVerification(true, "tracking-1"));
        when(individualRepository.findByNationalCode("1234567890")).thenReturn(Optional.empty());
        when(individualRepository.findByMobile("09121234567")).thenReturn(Optional.empty());
        when(individualRepository.save(any(IndividualPartyEntity.class))).thenAnswer(invocation -> {
            IndividualPartyEntity individual = invocation.getArgument(0);
            ReflectionTestUtils.setField(individual, "id", partyId);
            return individual;
        });
        when(profileRepository.findByPartyId(partyId)).thenReturn(Optional.empty());
        when(profileRepository.save(any(CustomerProfileEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByCode(IdentityRoles.CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(userRepository.save(any(UserAccountEntity.class))).thenAnswer(invocation -> {
            UserAccountEntity account = invocation.getArgument(0);
            ReflectionTestUtils.setField(account, "id", userId);
            return account;
        });

        var service = new DefaultCustomerRegistrationService(
                otpService,
                shahkarProvider,
                individualRepository,
                userRepository,
                roleRepository,
                profileRepository,
                new ClockTimeProvider(Clock.fixed(Instant.parse("2026-09-22T03:30:00Z"), ZoneOffset.UTC))
        );
        CustomerRegistrationResult result = service.verifyOtp(
                new VerifyCustomerRegistrationCommand(challengeId, "123456")
        );

        ArgumentCaptor<UserAccountEntity> accountCaptor = ArgumentCaptor.forClass(UserAccountEntity.class);
        verify(userRepository).save(accountCaptor.capture());
        UserAccountEntity account = accountCaptor.getValue();
        assertThat(account.getPrimaryAuthenticationMethod()).isEqualTo(AuthenticationMethod.OTP);
        assertThat(account.getUsername()).isNull();
        assertThat(account.getPasswordHash()).isNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.partyId()).isEqualTo(partyId);
        assertThat(result.onboardingStatus()).isEqualTo(CustomerOnboardingStatus.IDENTITY_VERIFIED);

        ArgumentCaptor<IndividualPartyEntity> individualCaptor = ArgumentCaptor.forClass(IndividualPartyEntity.class);
        verify(individualRepository).save(individualCaptor.capture());
        assertThat(individualCaptor.getValue().getShahkarStatus()).isEqualTo(ShahkarStatus.MATCHED);
        assertThat(individualCaptor.getValue().getShahkarTrackingId()).isEqualTo("tracking-1");
    }
}
