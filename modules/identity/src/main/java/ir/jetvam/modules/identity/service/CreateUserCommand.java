package ir.jetvam.modules.identity.service;

import ir.jetvam.common.security.UserCategory;

import java.time.LocalDate;
import java.util.Set;

/**
 * Carries the identity, credentials and access assignments for a new account.
 * The application service validates and canonicalizes every external value.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public record CreateUserCommand(
        String username,
        String mobile,
        String nationalCode,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String password,
        Set<UserCategory> categories,
        Set<String> roleCodes
) {
    public CreateUserCommand {
        categories = categories == null ? Set.of() : Set.copyOf(categories);
        roleCodes = roleCodes == null ? Set.of() : Set.copyOf(roleCodes);
    }
}
