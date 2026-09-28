package ir.jetvam.apps.uaa.user;

import ir.jetvam.modules.identity.service.UserView;

import java.util.UUID;

/**
 * Defines self-service operations for the authenticated account.
 * It keeps current-password verification outside the web controller.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
public interface CurrentAccountService {

    UserView get(UUID userId);

    void changePassword(UUID userId, String currentPassword, String newPassword);

    void logout(UUID userId);
}
