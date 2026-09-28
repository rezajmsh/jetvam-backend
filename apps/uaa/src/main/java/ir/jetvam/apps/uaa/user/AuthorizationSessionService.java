package ir.jetvam.apps.uaa.user;

import ir.jetvam.modules.identity.service.UserView;

/**
 * Removes renewable OAuth authorizations owned by a user account.
 * Credential and lifecycle changes use this port without knowing JDBC storage details.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
public interface AuthorizationSessionService {

    int revokeAll(UserView user);
}
