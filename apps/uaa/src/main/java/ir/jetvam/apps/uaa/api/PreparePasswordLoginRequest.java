package ir.jetvam.apps.uaa.api;

import jakarta.validation.constraints.NotBlank;

/**
 * Password credentials used only to prepare a system or merchant login transaction.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public record PreparePasswordLoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
