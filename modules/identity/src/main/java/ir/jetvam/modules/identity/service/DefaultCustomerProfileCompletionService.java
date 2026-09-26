package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.security.UserCategory;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.persistence.CustomerProfileEntity;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.repository.CustomerProfileRepository;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Completes canonical customer identity data independently from authentication and registration endpoints.
 * Only OTP customer accounts may update their own verified party and onboarding profile.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultCustomerProfileCompletionService implements CustomerProfileCompletionService {

    private final IndividualPartyRepository individualRepository;
    private final UserAccountRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final TimeProvider timeProvider;

    @Override
    @Transactional
    public CustomerProfileView completeIdentity(UUID userId, CompleteCustomerProfileCommand command) {
        Preconditions.requireNonNull(userId, "userId");
        Preconditions.requireNonNull(command, "command");
        String firstName = Preconditions.requireText(command.firstName(), "firstName").strip();
        String lastName = Preconditions.requireText(command.lastName(), "lastName").strip();
        LocalDate birthDate = Preconditions.requireNonNull(command.birthDate(), "birthDate");
        Preconditions.require(birthDate.isBefore(timeProvider.today()), "birthDate must be in the past");

        UserAccountEntity account = userRepository.findById(userId)
                .filter(candidate -> candidate.getPrimaryAuthenticationMethod() == AuthenticationMethod.OTP)
                .filter(candidate -> candidate.getCategories().contains(UserCategory.CUSTOMER))
                .orElseThrow(() -> new ResourceNotFoundException("customerUser", userId));
        UUID partyId = account.getParty().getId();
        IndividualPartyEntity individual = individualRepository.findById(partyId)
                .orElseThrow(() -> new ResourceNotFoundException("individualParty", partyId));
        CustomerProfileEntity profile = customerProfileRepository.findByPartyId(partyId)
                .orElseThrow(() -> new ResourceNotFoundException("customerProfile", partyId));

        individual.completeIdentity(firstName, lastName, birthDate);
        account.getParty().changeDisplayName(firstName + " " + lastName);
        profile.complete();
        return new CustomerProfileView(
                account.getId(),
                partyId,
                individual.getFirstName(),
                individual.getLastName(),
                individual.getBirthDate(),
                profile.getOnboardingStatus()
        );
    }
}
