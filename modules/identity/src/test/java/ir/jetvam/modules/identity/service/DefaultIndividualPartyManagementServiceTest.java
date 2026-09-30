package ir.jetvam.modules.identity.service;

import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies canonical party correction and synchronization with every linked account.
 * An identity correction must also release eligible customer onboarding.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
class DefaultIndividualPartyManagementServiceTest {

    @Test
    void updatesSharedIdentityAndSynchronizesLinkedPasswordAccount() {
        UUID partyId = UUID.randomUUID();
        IndividualPartyRepository individualRepository = mock(IndividualPartyRepository.class);
        UserAccountRepository userRepository = mock(UserAccountRepository.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        IndividualPartyEntity individual = mock(IndividualPartyEntity.class);
        UserAccountEntity passwordAccount = mock(UserAccountEntity.class);
        UserAccountEntity otpAccount = mock(UserAccountEntity.class);
        Instant now = Instant.parse("2026-09-28T08:00:00Z");

        when(individualRepository.findById(partyId)).thenReturn(Optional.of(individual));
        when(individual.getId()).thenReturn(partyId);
        when(userRepository.findAllByParty_Id(partyId)).thenReturn(List.of(passwordAccount, otpAccount));
        when(passwordAccount.getPrimaryAuthenticationMethod()).thenReturn(AuthenticationMethod.PASSWORD);
        when(otpAccount.getPrimaryAuthenticationMethod()).thenReturn(AuthenticationMethod.OTP);
        when(timeProvider.today()).thenReturn(LocalDate.of(2026, 9, 28));
        when(timeProvider.now()).thenReturn(now);

        DefaultIndividualPartyManagementService service = new DefaultIndividualPartyManagementService(
                individualRepository, userRepository, timeProvider
        );
        UpdateIndividualPartyCommand command = new UpdateIndividualPartyCommand(
                "0067749828", "رضا", "جمشیدی", LocalDate.of(1990, 5, 20), "09121234567"
        );

        IndividualPartyView result = service.update(partyId, command);

        verify(individual).updateIdentity(
                "0067749828", "رضا", "جمشیدی", LocalDate.of(1990, 5, 20), "09121234567"
        );
        verify(passwordAccount).changeAuthenticationMobile("09121234567", now);
        assertThat(result.id()).isEqualTo(partyId);
    }
}
