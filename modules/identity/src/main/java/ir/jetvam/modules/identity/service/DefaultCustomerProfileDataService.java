package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.OperationNotAllowedException;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.model.CustomerOnboardingStatus;
import ir.jetvam.modules.identity.persistence.CustomerProfileEntity;
import ir.jetvam.modules.identity.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Persists customer profile information in typed relational fields and exposes reusable revisions.
 * Access is scoped by the supplied party identifier so callers can enforce customer ownership.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultCustomerProfileDataService implements CustomerProfileDataService {

    private final CustomerProfileRepository profileRepository;

    @Override
    @Transactional(readOnly = true)
    public CustomerProfileModels.ProfileView get(UUID customerPartyId) {
        return toView(findCompleted(customerPartyId));
    }

    @Override
    @Transactional
    public CustomerProfileModels.ProfileView updatePersonalInformation(
            UUID customerPartyId,
            CustomerProfileModels.UpdatePersonalInformation command
    ) {
        Preconditions.requireNonNull(command, "command");
        CustomerProfileEntity profile = findCompleted(customerPartyId);
        profile.updatePersonalInformation(
                command.bankCardNumber(), command.landline(), command.postalCode(), command.address()
        );
        return toView(profile);
    }

    @Override
    @Transactional
    public CustomerProfileModels.ProfileView updateEmploymentInformation(
            UUID customerPartyId,
            CustomerProfileModels.UpdateEmploymentInformation command
    ) {
        Preconditions.requireNonNull(command, "command");
        CustomerProfileEntity profile = findCompleted(customerPartyId);
        profile.updateEmploymentInformation(
                command.educationCode(), command.employmentCode(), command.monthlyIncome(), command.documentIds()
        );
        return toView(profile);
    }

    private CustomerProfileEntity findCompleted(UUID customerPartyId) {
        UUID requiredPartyId = Preconditions.requireNonNull(customerPartyId, "customerPartyId");
        CustomerProfileEntity profile = profileRepository.findByPartyId(requiredPartyId)
                .orElseThrow(() -> new ResourceNotFoundException("customerProfile", requiredPartyId));
        if (profile.getOnboardingStatus() != CustomerOnboardingStatus.COMPLETED) {
            throw new OperationNotAllowedException(
                    "manage-customer-profile",
                    "Customer identity profile must be completed first"
            );
        }
        return profile;
    }

    private static CustomerProfileModels.ProfileView toView(CustomerProfileEntity profile) {
        CustomerProfileModels.PersonalInformation personal = profile.hasPersonalInformation()
                ? new CustomerProfileModels.PersonalInformation(
                        profile.getBankCardNumber(), profile.getLandline(), profile.getPostalCode(),
                        profile.getAddress(), profile.getPersonalInformationRevision()
                )
                : null;
        CustomerProfileModels.EmploymentInformation employment = profile.hasEmploymentInformation()
                ? new CustomerProfileModels.EmploymentInformation(
                        profile.getEducationCode(), profile.getEmploymentCode(), profile.getMonthlyIncome(),
                        List.copyOf(profile.getEmploymentDocumentIds()), profile.getEmploymentInformationRevision()
                )
                : null;
        return new CustomerProfileModels.ProfileView(profile.getId(), personal, employment);
    }
}
