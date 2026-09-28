package ir.jetvam.apps.uaa.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Carries a replacement password supplied by an authorized administrator.
 * The credential is never returned or included in audit attributes.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
public record ResetUserPasswordRequest(
        @NotBlank @Size(min = 10, max = 200) String newPassword
) {
}
