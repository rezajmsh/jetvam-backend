package ir.jetvam.modules.identity.service;

import java.util.UUID;

/**
 * Completes the verified customer's canonical identity profile after registration.
 * The authenticated user identifier is resolved to its owning customer party.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface CustomerProfileCompletionService {

    CustomerProfileView completeIdentity(UUID userId, CompleteCustomerProfileCommand command);
}
