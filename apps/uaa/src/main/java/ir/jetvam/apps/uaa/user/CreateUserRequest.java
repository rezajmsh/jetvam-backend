package ir.jetvam.apps.uaa.user;

import ir.jetvam.common.security.UserCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

/**
 * Defines the administrative HTTP payload for creating a user account.
 * Validation covers transport requirements before domain normalization runs.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public record CreateUserRequest(
        String username,
        String mobile,
        @NotBlank String nationalCode,
        @NotBlank String firstName,
        @NotBlank String lastName,
        LocalDate birthDate,
        @NotBlank @Size(min = 10, max = 200) String password,
        @NotEmpty Set<UserCategory> categories,
        Set<String> roles
) {
}
