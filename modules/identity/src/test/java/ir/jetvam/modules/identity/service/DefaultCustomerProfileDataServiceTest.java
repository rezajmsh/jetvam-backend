package ir.jetvam.modules.identity.service;

import ir.jetvam.modules.identity.model.IdentityVerificationStatus;
import ir.jetvam.modules.identity.model.MobileVerificationStatus;
import ir.jetvam.modules.identity.model.ShahkarStatus;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that customer profile data is sourced from the canonical individual Party.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
class DefaultCustomerProfileDataServiceTest {
    private final IndividualPartyRepository repository = mock(IndividualPartyRepository.class);
    private final DefaultCustomerProfileDataService service = new DefaultCustomerProfileDataService(repository);

    @Test
    void exposesIndependentVerificationAndCompletenessStates() {
        UUID partyId = UUID.randomUUID();
        IndividualPartyEntity party = mock(IndividualPartyEntity.class);
        when(repository.findById(partyId)).thenReturn(Optional.of(party));
        when(party.getId()).thenReturn(partyId);
        when(party.getNationalCode()).thenReturn("0012345678");
        when(party.getFirstName()).thenReturn("علی");
        when(party.getLastName()).thenReturn("احمدی");
        when(party.getBirthDate()).thenReturn(LocalDate.of(1990, 5, 20));
        when(party.getMobileVerificationStatus()).thenReturn(MobileVerificationStatus.VERIFIED);
        when(party.getShahkarStatus()).thenReturn(ShahkarStatus.MATCHED);
        when(party.getIdentityVerificationStatus()).thenReturn(IdentityVerificationStatus.VERIFIED);
        when(party.hasCompleteIdentityInformation()).thenReturn(true);

        CustomerProfileModels.ProfileView result = service.get(partyId);

        assertThat(result.identityInformationComplete()).isTrue();
        assertThat(result.mobileVerificationStatus()).isEqualTo(MobileVerificationStatus.VERIFIED);
        assertThat(result.identityInformation().firstName()).isEqualTo("علی");
    }

    @Test
    void updatesPersonalInformationOnParty() {
        UUID partyId = UUID.randomUUID();
        IndividualPartyEntity party = mock(IndividualPartyEntity.class);
        when(repository.findById(partyId)).thenReturn(Optional.of(party));
        when(party.getId()).thenReturn(partyId);

        var command = new CustomerProfileModels.UpdatePersonalInformation(
                "6037991234567890", "02112345678", "1234567890", "تهران"
        );
        service.updatePersonalInformation(partyId, command);

        verify(party).updatePersonalInformation(
                "6037991234567890", "02112345678", "1234567890", "تهران"
        );
    }
}
