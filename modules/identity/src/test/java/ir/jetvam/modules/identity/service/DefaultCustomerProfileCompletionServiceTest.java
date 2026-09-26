package ir.jetvam.modules.identity.service;

import ir.jetvam.common.security.UserCategory;
import ir.jetvam.common.time.ClockTimeProvider;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.model.CustomerOnboardingStatus;
import ir.jetvam.modules.identity.persistence.CustomerProfileEntity;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.persistence.PartyEntity;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.repository.CustomerProfileRepository;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that identity completion updates the canonical party and customer onboarding profile.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
class DefaultCustomerProfileCompletionServiceTest {

    @Test
    void completesCanonicalIdentityForAuthenticatedOtpCustomer() {
        UUID userId = UUID.randomUUID();
        UUID partyId = UUID.randomUUID();
        LocalDate birthDate = LocalDate.of(1990, 5, 10);
        PartyEntity party = mock(PartyEntity.class);
        UserAccountEntity account = mock(UserAccountEntity.class);
        IndividualPartyEntity individual = mock(IndividualPartyEntity.class);
        CustomerProfileEntity profile = mock(CustomerProfileEntity.class);
        UserAccountRepository userRepository = mock(UserAccountRepository.class);
        IndividualPartyRepository individualRepository = mock(IndividualPartyRepository.class);
        CustomerProfileRepository profileRepository = mock(CustomerProfileRepository.class);

        when(account.getId()).thenReturn(userId);
        when(account.getParty()).thenReturn(party);
        when(account.getPrimaryAuthenticationMethod()).thenReturn(AuthenticationMethod.OTP);
        when(account.getCategories()).thenReturn(Set.of(UserCategory.CUSTOMER));
        when(party.getId()).thenReturn(partyId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(account));
        when(individualRepository.findById(partyId)).thenReturn(Optional.of(individual));
        when(profileRepository.findByPartyId(partyId)).thenReturn(Optional.of(profile));
        when(individual.getFirstName()).thenReturn("Reza");
        when(individual.getLastName()).thenReturn("Jamshidi");
        when(individual.getBirthDate()).thenReturn(birthDate);
        when(profile.getOnboardingStatus()).thenReturn(CustomerOnboardingStatus.COMPLETED);

        var service = new DefaultCustomerProfileCompletionService(
                individualRepository,
                userRepository,
                profileRepository,
                new ClockTimeProvider(Clock.fixed(Instant.parse("2026-09-25T12:00:00Z"), ZoneOffset.UTC))
        );

        CustomerProfileView result = service.completeIdentity(
                userId,
                new CompleteCustomerProfileCommand(" Reza ", " Jamshidi ", birthDate)
        );

        assertThat(result).isEqualTo(new CustomerProfileView(
                userId,
                partyId,
                "Reza",
                "Jamshidi",
                birthDate,
                CustomerOnboardingStatus.COMPLETED
        ));
        verify(individual).completeIdentity("Reza", "Jamshidi", birthDate);
        verify(party).changeDisplayName("Reza Jamshidi");
        verify(profile).complete();
    }
}
