package ir.jetvam.modules.identity.service;

import ir.jetvam.common.security.UserCategory;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.modules.identity.model.AuthenticationMethod;

import java.util.Set;
import java.util.UUID;

/**
 * Exposes a safe account-management view without credential or persistence data.
 * It contains the party link and effective role assignments.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public record UserView(
        UUID id,
        UUID partyId,
        String displayName,
        String username,
        String mobile,
        AuthenticationMethod primaryAuthenticationMethod,
        UserAccountStatus status,
        Set<UserCategory> categories,
        Set<String> roles
) {
}
