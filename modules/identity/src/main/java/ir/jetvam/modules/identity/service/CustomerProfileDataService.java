package ir.jetvam.modules.identity.service;

import java.util.UUID;

/**
 * Owns reusable personal and employment profile information for a verified customer.
 * Consuming workflows may snapshot returned revisions but must not persist duplicate profile payloads.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface CustomerProfileDataService {

    CustomerProfileModels.ProfileView get(UUID customerPartyId);

    CustomerProfileModels.ProfileView updatePersonalInformation(
            UUID customerPartyId,
            CustomerProfileModels.UpdatePersonalInformation command
    );

    CustomerProfileModels.ProfileView updateEmploymentInformation(
            UUID customerPartyId,
            CustomerProfileModels.UpdateEmploymentInformation command
    );
}
