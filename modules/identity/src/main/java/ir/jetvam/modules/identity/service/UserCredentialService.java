package ir.jetvam.modules.identity.service;

import java.util.UUID;

/**
 * Defines password lifecycle operations independently from user provisioning.
 * Implementations never expose encoded credentials to callers.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
public interface UserCredentialService {

    UserView changePassword(UUID userId, String currentPassword, String newPassword);

    UserView resetPassword(UUID userId, String newPassword);
}
