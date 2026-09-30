package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.OperationNotAllowedException;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.persistence.PartyEntity;
import ir.jetvam.modules.identity.repository.PartyRepository;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Optional;

/**
 * Loads completed customer identity data for downstream business workflows.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultCustomerIdentityQueryService implements CustomerIdentityQueryService {

    private final PartyRepository partyRepository;
    private final IndividualPartyRepository individualPartyRepository;

    @Override
    @Transactional(readOnly = true)
    public CustomerIdentityFacts getVerifiedCustomer(UUID partyId) {
        UUID requiredPartyId = Preconditions.requireNonNull(partyId, "partyId");
        PartyEntity party = partyRepository.findById(requiredPartyId)
                .orElseThrow(() -> new ResourceNotFoundException("party", requiredPartyId));
        if (!(party instanceof IndividualPartyEntity individual)) {
            throw new OperationNotAllowedException("start-loan-application", "Party is not an individual customer");
        }
        if (!individual.isReadyForApplication()) {
            throw new OperationNotAllowedException(
                    "start-loan-application",
                    "Customer identity and personal information must be completed before applying"
            );
        }
        Preconditions.requireNonNull(individual.getBirthDate(), "customer.birthDate");
        return new CustomerIdentityFacts(
                requiredPartyId,
                individual.getNationalCode(),
                individual.getFirstName(),
                individual.getLastName(),
                individual.getBirthDate(),
                individual.getMobile()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> findIndividualPartyId(String nationalCode, String mobile) {
        String requiredNationalCode = Preconditions.requireText(nationalCode, "nationalCode").strip();
        String requiredMobile = Preconditions.requireText(mobile, "mobile").strip();
        return individualPartyRepository.findByNationalCode(requiredNationalCode)
                .filter(individual -> requiredMobile.equals(individual.getMobile()))
                .map(IndividualPartyEntity::getId);
    }
}
