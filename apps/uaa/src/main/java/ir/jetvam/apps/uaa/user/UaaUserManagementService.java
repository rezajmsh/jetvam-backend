package ir.jetvam.apps.uaa.user;

import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.modules.identity.service.CreateAccountForPartyCommand;
import ir.jetvam.modules.identity.service.CreateUserCommand;
import ir.jetvam.modules.identity.service.IndividualPartyView;
import ir.jetvam.modules.identity.service.UpdateIndividualPartyCommand;
import ir.jetvam.modules.identity.service.UserPage;
import ir.jetvam.modules.identity.service.UserSearchQuery;
import ir.jetvam.modules.identity.service.UserView;

import java.util.UUID;

/**
 * Coordinates administrative account operations with session revocation and audit.
 * Controllers use this boundary instead of composing sensitive operations themselves.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
public interface UaaUserManagementService {

    UserView create(UUID actorUserId, CreateUserCommand command);

    UserView createForParty(UUID actorUserId, CreateAccountForPartyCommand command);

    UserPage search(UserSearchQuery query);

    UserView get(UUID userId);

    IndividualPartyView getParty(UUID partyId);

    IndividualPartyView updateParty(UUID actorUserId, UUID partyId, UpdateIndividualPartyCommand command);

    UserView changeStatus(UUID actorUserId, UUID userId, UserAccountStatus status);

    UserView unlock(UUID actorUserId, UUID userId);

    UserView resetPassword(UUID actorUserId, UUID userId, String newPassword);
}
