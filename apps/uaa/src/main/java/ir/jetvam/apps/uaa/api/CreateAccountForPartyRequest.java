package ir.jetvam.apps.uaa.api;

import ir.jetvam.common.security.UserCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Defines administrative creation of a password account for an existing party.
 * Canonical identity data is not duplicated in the request.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public record CreateAccountForPartyRequest(
        @NotBlank String username,
        @NotBlank String mobile,
        @NotBlank @Size(min = 10, max = 200) String password,
        @NotEmpty Set<UserCategory> categories,
        Set<String> roles
) {
}
