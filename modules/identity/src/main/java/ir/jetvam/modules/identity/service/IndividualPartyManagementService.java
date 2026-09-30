package ir.jetvam.modules.identity.service;

import java.util.UUID;

/**
 * Manages canonical individual identity independently from authentication accounts.
 * Updates affect every account linked to the same party.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
public interface IndividualPartyManagementService {

    IndividualPartyView get(UUID partyId);

    IndividualPartyView update(UUID partyId, UpdateIndividualPartyCommand command);
}
