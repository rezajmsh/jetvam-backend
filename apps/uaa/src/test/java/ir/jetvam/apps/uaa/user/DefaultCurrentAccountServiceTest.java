package ir.jetvam.apps.uaa.user;

import ir.jetvam.infra.observability.audit.AuditLogger;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.modules.identity.service.UserAccountService;
import ir.jetvam.modules.identity.service.UserCredentialService;
import ir.jetvam.modules.identity.service.UserView;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that a self-service password change also revokes renewable sessions.
 * The test protects the orchestration around the identity credential service.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
class DefaultCurrentAccountServiceTest {

    @Test
    void changesPasswordThenRevokesAuthorizationsAndAudits() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000701");
        UserView user = new UserView(
                userId,
                UUID.fromString("00000000-0000-0000-0000-000000000702"),
                "Current User",
                "current.user",
                "09121234567",
                AuthenticationMethod.PASSWORD,
                UserAccountStatus.ACTIVE,
                Set.of(),
                Set.of()
        );
        UserAccountService accountService = mock(UserAccountService.class);
        UserCredentialService credentialService = mock(UserCredentialService.class);
        AuthorizationSessionService sessionService = mock(AuthorizationSessionService.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        when(credentialService.changePassword(userId, "old-password", "new-password")).thenReturn(user);

        new DefaultCurrentAccountService(accountService, credentialService, sessionService, auditLogger)
                .changePassword(userId, "old-password", "new-password");

        verify(sessionService).revokeAll(user);
        verify(auditLogger).record(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void revokesAuthorizationsAndAuditsLogout() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000703");
        UserView user = new UserView(
                userId,
                UUID.fromString("00000000-0000-0000-0000-000000000704"),
                "Current User",
                "current.user",
                "09121234567",
                AuthenticationMethod.PASSWORD,
                UserAccountStatus.ACTIVE,
                Set.of(),
                Set.of()
        );
        UserAccountService accountService = mock(UserAccountService.class);
        UserCredentialService credentialService = mock(UserCredentialService.class);
        AuthorizationSessionService sessionService = mock(AuthorizationSessionService.class);
        AuditLogger auditLogger = mock(AuditLogger.class);
        when(accountService.get(userId)).thenReturn(user);

        new DefaultCurrentAccountService(accountService, credentialService, sessionService, auditLogger)
                .logout(userId);

        verify(sessionService).revokeAll(user);
        verify(auditLogger).record(org.mockito.ArgumentMatchers.any());
    }
}
