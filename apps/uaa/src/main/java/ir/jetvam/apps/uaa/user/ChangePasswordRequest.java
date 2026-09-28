package ir.jetvam.apps.uaa.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Carries the current and replacement password for an authenticated user.
 * Transport validation bounds credential size before password hashing occurs.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
public record ChangePasswordRequest(
        @NotBlank @Size(max = 200) String currentPassword,
        @NotBlank @Size(min = 10, max = 200) String newPassword
) {
}
