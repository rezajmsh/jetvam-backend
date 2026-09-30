package ir.jetvam.apps.uaa.user;

import ir.jetvam.common.exception.OperationNotAllowedException;
import ir.jetvam.infra.observability.audit.AuditEvent;
import ir.jetvam.infra.observability.audit.AuditLogger;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.modules.identity.service.CreateAccountForPartyCommand;
import ir.jetvam.modules.identity.service.CreateUserCommand;
import ir.jetvam.modules.identity.service.IndividualPartyManagementService;
import ir.jetvam.modules.identity.service.IndividualPartyView;
import ir.jetvam.modules.identity.service.UpdateIndividualPartyCommand;
import ir.jetvam.modules.identity.service.UserAccountService;
import ir.jetvam.modules.identity.service.UserCredentialService;
import ir.jetvam.modules.identity.service.UserPage;
import ir.jetvam.modules.identity.service.UserSearchQuery;
import ir.jetvam.modules.identity.service.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Applies UAA security invariants around identity account administration.
 * Sensitive mutations are audited and invalidate every renewable authorization.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DefaultUaaUserManagementService implements UaaUserManagementService {

    private final UserAccountService userAccountService;
    private final UserCredentialService userCredentialService;
    private final IndividualPartyManagementService partyManagementService;
    private final AuthorizationSessionService authorizationSessionService;
    private final AuditLogger auditLogger;

    @Override
    public UserView create(UUID actorUserId, CreateUserCommand command) {
        UserView user = userAccountService.create(command);
        audit("identity.user.created", actorUserId, user.id(), Map.of("status", user.status().name()));
        return user;
    }

    @Override
    public UserView createForParty(UUID actorUserId, CreateAccountForPartyCommand command) {
        UserView user = userAccountService.createForParty(command);
        audit("identity.user.created-for-party", actorUserId, user.id(),
                Map.of("party.id", user.partyId().toString()));
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public UserPage search(UserSearchQuery query) {
        return userAccountService.search(query);
    }

    @Override
    @Transactional(readOnly = true)
    public UserView get(UUID userId) {
        return userAccountService.get(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public IndividualPartyView getParty(UUID partyId) {
        return partyManagementService.get(partyId);
    }

    @Override
    public IndividualPartyView updateParty(
            UUID actorUserId,
            UUID partyId,
            UpdateIndividualPartyCommand command
    ) {
        IndividualPartyView party = partyManagementService.update(partyId, command);
        auditLogger.record(AuditEvent.builder()
                .action("identity.party.updated")
                .outcome("success")
                .actorType("USER")
                .actorId(actorUserId.toString())
                .subjectType("PARTY")
                .subjectId(partyId.toString())
                .attributes(Map.of())
                .build());
        return party;
    }

    @Override
    public UserView changeStatus(UUID actorUserId, UUID userId, UserAccountStatus status) {
        if (actorUserId.equals(userId) && status != UserAccountStatus.ACTIVE) {
            throw new OperationNotAllowedException(
                    "change-own-account-status",
                    "Administrators cannot deactivate their own account"
            );
        }
        UserView user = userAccountService.changeStatus(userId, status);
        int revoked = status == UserAccountStatus.DISABLED
                ? authorizationSessionService.revokeAll(user)
                : 0;
        audit("identity.user.status-changed", actorUserId, user.id(), Map.of(
                "status", status.name(),
                "revoked.authorization.count", revoked
        ));
        return user;
    }

    @Override
    public UserView unlock(UUID actorUserId, UUID userId) {
        UserView user = userAccountService.unlock(userId);
        int revoked = authorizationSessionService.revokeAll(user);
        audit("identity.user.unlocked", actorUserId, user.id(),
                Map.of("revoked.authorization.count", revoked));
        return user;
    }

    @Override
    public UserView resetPassword(UUID actorUserId, UUID userId, String newPassword) {
        if (actorUserId.equals(userId)) {
            throw new OperationNotAllowedException(
                    "administrative-own-password-reset",
                    "Use the current-account password endpoint to change your own password"
            );
        }
        UserView user = userCredentialService.resetPassword(userId, newPassword);
        int revoked = authorizationSessionService.revokeAll(user);
        audit("identity.user.password-reset", actorUserId, user.id(),
                Map.of("revoked.authorization.count", revoked));
        return user;
    }

    private void audit(String action, UUID actorUserId, UUID subjectUserId, Map<String, ?> attributes) {
        auditLogger.record(AuditEvent.builder()
                .action(action)
                .outcome("success")
                .actorType("USER")
                .actorId(actorUserId.toString())
                .subjectType("USER_ACCOUNT")
                .subjectId(subjectUserId.toString())
                .attributes(attributes)
                .build());
    }
}
