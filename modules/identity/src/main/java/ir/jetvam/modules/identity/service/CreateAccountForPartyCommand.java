package ir.jetvam.modules.identity.service;

import ir.jetvam.common.security.UserCategory;

import java.util.Set;
import java.util.UUID;

/**
 * Carries password-account settings for a party that already exists.
 * It avoids duplicating canonical identity records during user administration.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record CreateAccountForPartyCommand(
        UUID partyId,
        String username,
        String mobile,
        String password,
        Set<UserCategory> categories,
        Set<String> roleCodes
) {
    public CreateAccountForPartyCommand {
        categories = categories == null ? Set.of() : Set.copyOf(categories);
        roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
    }
}
