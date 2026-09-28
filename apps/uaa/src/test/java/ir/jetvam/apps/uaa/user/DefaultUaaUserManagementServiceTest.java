package ir.jetvam.apps.uaa.user;

import ir.jetvam.common.exception.OperationNotAllowedException;
import ir.jetvam.infra.observability.audit.AuditLogger;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.modules.identity.service.UserAccountService;
import ir.jetvam.modules.identity.service.UserCredentialService;
import ir.jetvam.modules.identity.service.UserView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies administrative safeguards, revocation and audit coordination.
 * Self-deactivation and self-reset must be rejected before account mutation.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
class DefaultUaaUserManagementServiceTest {

    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000801");
    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000802");
    private static final UUID PARTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000803");

    private UserAccountService userAccountService;
    private UserCredentialService credentialService;
    private AuthorizationSessionService sessionService;
    private AuditLogger auditLogger;
    private DefaultUaaUserManagementService service;

    @BeforeEach
    void setUp() {
        userAccountService = mock(UserAccountService.class);
        credentialService = mock(UserCredentialService.class);
        sessionService = mock(AuthorizationSessionService.class);
        auditLogger = mock(AuditLogger.class);
        service = new DefaultUaaUserManagementService(
                userAccountService,
                credentialService,
                sessionService,
                auditLogger
        );
    }

    @Test
    void disablesAccountAndRevokesRenewableAuthorizations() {
        UserView disabled = user(UserAccountStatus.DISABLED);
        when(userAccountService.changeStatus(TARGET_ID, UserAccountStatus.DISABLED)).thenReturn(disabled);
        when(sessionService.revokeAll(disabled)).thenReturn(2);

        service.changeStatus(ACTOR_ID, TARGET_ID, UserAccountStatus.DISABLED);

        verify(sessionService).revokeAll(disabled);
        verify(auditLogger).record(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsSelfDeactivationBeforeMutation() {
        assertThatThrownBy(() -> service.changeStatus(ACTOR_ID, ACTOR_ID, UserAccountStatus.DISABLED))
                .isInstanceOf(OperationNotAllowedException.class);

        verify(userAccountService, never()).changeStatus(ACTOR_ID, UserAccountStatus.DISABLED);
    }

    @Test
    void rejectsAdministrativePasswordResetForOwnAccount() {
        assertThatThrownBy(() -> service.resetPassword(ACTOR_ID, ACTOR_ID, "new-password"))
                .isInstanceOf(OperationNotAllowedException.class);

        verify(credentialService, never()).resetPassword(ACTOR_ID, "new-password");
    }

    private static UserView user(UserAccountStatus status) {
        return new UserView(
                TARGET_ID,
                PARTY_ID,
                "Target User",
                "target.user",
                "09121234567",
                AuthenticationMethod.PASSWORD,
                status,
                Set.of(),
                Set.of()
        );
    }
}
