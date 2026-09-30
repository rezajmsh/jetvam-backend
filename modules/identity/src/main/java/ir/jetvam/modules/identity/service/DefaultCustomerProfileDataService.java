package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Exposes and updates canonical individual-party identity and personal information.
 * Employment and document data intentionally belong to individual loan applications.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultCustomerProfileDataService implements CustomerProfileDataService {

    private final IndividualPartyRepository individualPartyRepository;

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileModels.ProfileView get(UUID customerPartyId) {
        return toView(findIndividual(customerPartyId));
    }

    @Override
    @Transactional
    public CustomerProfileModels.ProfileView updatePersonalInformation(
            UUID customerPartyId,
            CustomerProfileModels.UpdatePersonalInformation command
    ) {
        Preconditions.requireNonNull(command, "command");
        IndividualPartyEntity individual = findIndividual(customerPartyId);
        individual.updatePersonalInformation(
                command.bankCardNumber(), command.landline(), command.postalCode(), command.address()
        );
        return toView(individual);
    }

    private IndividualPartyEntity findIndividual(UUID customerPartyId) {
        return individualPartyRepository.findById(customerPartyId)
                .orElseThrow(() -> new ResourceNotFoundException("individualParty", customerPartyId));
    }

    private static CustomerProfileModels.ProfileView toView(IndividualPartyEntity individual) {
        CustomerProfileModels.IdentityInformation identity = new CustomerProfileModels.IdentityInformation(
                individual.getNationalCode(), individual.getFirstName(),
                individual.getLastName(), individual.getBirthDate()
        );
        CustomerProfileModels.PersonalInformation personal = individual.hasCompletePersonalInformation()
                ? new CustomerProfileModels.PersonalInformation(
                        individual.getBankCardNumber(), individual.getLandline(), individual.getPostalCode(),
                        individual.getAddress()
                )
                : null;
        return new CustomerProfileModels.ProfileView(
                individual.getId(), individual.getMobileVerificationStatus(), individual.getShahkarStatus(),
                individual.getIdentityVerificationStatus(), individual.hasCompleteIdentityInformation(),
                individual.hasCompletePersonalInformation(), individual.isReadyForApplication(), identity, personal
        );
    }
}
