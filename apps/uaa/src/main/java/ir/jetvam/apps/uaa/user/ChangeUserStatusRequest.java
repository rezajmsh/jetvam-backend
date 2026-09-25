package ir.jetvam.apps.uaa.user;

import ir.jetvam.modules.identity.model.UserAccountStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Carries an explicit lifecycle transition requested for a user account.
 * Keeping status changes separate avoids accidental credential replacement.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public record ChangeUserStatusRequest(@NotNull UserAccountStatus status) {
}
