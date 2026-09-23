package ir.jetvam.modules.identity.service;

import ir.jetvam.modules.identity.model.UserAccountStatus;

import java.util.UUID;

/**
 * Defines account-management use cases exposed by the identity module.
 * Callers depend on this port rather than persistence or implementation details.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public interface UserAccountService {

    UserView create(CreateUserCommand command);

    UserView createForParty(CreateAccountForPartyCommand command);

    UserView get(UUID id);

    UserView changeStatus(UUID id, UserAccountStatus status);

    boolean usernameExists(String username);
}
