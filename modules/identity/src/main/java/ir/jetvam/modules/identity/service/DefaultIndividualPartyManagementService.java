package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.ConflictException;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.common.validation.ValidationCollector;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Applies validated party corrections and keeps linked password-account mobile data synchronized.
 * Completing all identity fields also completes an eligible customer onboarding profile.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultIndividualPartyManagementService implements IndividualPartyManagementService {

    private final IndividualPartyRepository individualRepository;
    private final UserAccountRepository userRepository;
    private final TimeProvider timeProvider;

    @Override
    @Transactional(readOnly = true)
    public IndividualPartyView get(UUID partyId) {
        return toView(find(partyId));
    }

    @Override
    @Transactional
    public IndividualPartyView update(UUID partyId, UpdateIndividualPartyCommand command) {
        Preconditions.requireNonNull(command, "command");
        IndividualPartyEntity individual = find(partyId);
        String nationalCode = IranianIdentifiers.normalizeNationalCode(command.nationalCode());
        String mobile = IranianIdentifiers.normalizeMobileNumber(command.mobile());
        String firstName = command.firstName() == null ? null : command.firstName().strip();
        String lastName = command.lastName() == null ? null : command.lastName().strip();
        LocalDate birthDate = command.birthDate();

        ValidationCollector.create()
                .require(IranianIdentifiers.isValidNationalCode(nationalCode), "nationalCode", "INVALID",
                        "nationalCode is invalid")
                .require(IranianIdentifiers.isValidMobileNumber(mobile), "mobile", "INVALID",
                        "mobile is invalid")
                .require(firstName != null && !firstName.isBlank(), "firstName", "REQUIRED",
                        "firstName is required")
                .require(lastName != null && !lastName.isBlank(), "lastName", "REQUIRED",
                        "lastName is required")
                .require(birthDate != null && birthDate.isBefore(timeProvider.today()), "birthDate", "INVALID",
                        "birthDate must be in the past")
                .throwIfInvalid("Individual party identity is invalid");

        if (individualRepository.existsByNationalCodeAndIdNot(nationalCode, individual.getId())) {
            throw new ConflictException("National code belongs to another party", Map.of("nationalCode", nationalCode));
        }
        if (individualRepository.existsByMobileAndIdNot(mobile, individual.getId())) {
            throw new ConflictException("Mobile belongs to another party", Map.of("mobile", mobile));
        }

        individual.updateIdentity(nationalCode, firstName, lastName, birthDate, mobile);
        userRepository.findAllByParty_Id(individual.getId()).stream()
                .filter(account -> account.getPrimaryAuthenticationMethod() == AuthenticationMethod.PASSWORD)
                .forEach(account -> account.changeAuthenticationMobile(mobile, timeProvider.now()));
        return toView(individual);
    }

    private IndividualPartyEntity find(UUID partyId) {
        UUID requiredPartyId = Preconditions.requireNonNull(partyId, "partyId");
        return individualRepository.findById(requiredPartyId)
                .orElseThrow(() -> new ResourceNotFoundException("individualParty", requiredPartyId));
    }

    private static IndividualPartyView toView(IndividualPartyEntity individual) {
        return new IndividualPartyView(
                individual.getId(), individual.getDisplayName(), individual.getNationalCode(),
                individual.getFirstName(), individual.getLastName(), individual.getBirthDate(), individual.getMobile()
        );
    }
}
