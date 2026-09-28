package ir.jetvam.apps.uaa.user;

import ir.jetvam.infra.observability.audit.AuditEvent;
import ir.jetvam.infra.observability.audit.AuditLogger;
import ir.jetvam.modules.identity.service.UserAccountService;
import ir.jetvam.modules.identity.service.UserCredentialService;
import ir.jetvam.modules.identity.service.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Executes authenticated self-service operations and revokes renewable sessions.
 * Successful password changes require the client to authenticate again.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DefaultCurrentAccountService implements CurrentAccountService {

    private final UserAccountService userAccountService;
    private final UserCredentialService userCredentialService;
    private final AuthorizationSessionService authorizationSessionService;
    private final AuditLogger auditLogger;

    @Override
    @Transactional(readOnly = true)
    public UserView get(UUID userId) {
        return userAccountService.get(userId);
    }

    @Override
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        UserView user = userCredentialService.changePassword(userId, currentPassword, newPassword);
        int revoked = authorizationSessionService.revokeAll(user);
        auditLogger.record(AuditEvent.builder()
                .action("identity.account.password-changed")
                .outcome("success")
                .actorType("USER")
                .actorId(userId.toString())
                .subjectType("USER_ACCOUNT")
                .subjectId(userId.toString())
                .attributes(Map.of("revoked.authorization.count", revoked))
                .build());
    }

    @Override
    public void logout(UUID userId) {
        UserView user = userAccountService.get(userId);
        int revoked = authorizationSessionService.revokeAll(user);
        auditLogger.record(AuditEvent.builder()
                .action("identity.account.logged-out")
                .outcome("success")
                .actorType("USER")
                .actorId(userId.toString())
                .subjectType("USER_ACCOUNT")
                .subjectId(userId.toString())
                .attributes(Map.of("revoked.authorization.count", revoked))
                .build());
    }
}
