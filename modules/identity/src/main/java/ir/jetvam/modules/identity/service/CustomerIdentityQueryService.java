package ir.jetvam.modules.identity.service;

import java.util.UUID;
import java.util.Optional;

/**
 * Provides an authoritative read boundary for customer identity facts used by loan workflows.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface CustomerIdentityQueryService {

    CustomerIdentityFacts getVerifiedCustomer(UUID partyId);

    Optional<UUID> findIndividualPartyId(String nationalCode, String mobile);
}
