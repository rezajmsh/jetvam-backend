package ir.jetvam.apps.uaa.persistence;

import ir.jetvam.apps.uaa.user.AuthorizationSessionService;
import ir.jetvam.modules.identity.service.UserView;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

/**
 * Revokes Spring Authorization Server records using its supported JDBC schema.
 * JdbcOperations calls remain covered by the shared repository observability aspect.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
@Repository
@RequiredArgsConstructor
public class JdbcAuthorizationSessionService implements AuthorizationSessionService {

    private static final String DELETE_BY_PRINCIPAL =
            "delete from oauth2_authorization where principal_name = ?";

    private final JdbcOperations jdbcOperations;

    @Override
    public int revokeAll(UserView user) {
        String principalName = user.username() == null || user.username().isBlank()
                ? user.partyId().toString()
                : user.username();
        return jdbcOperations.update(DELETE_BY_PRINCIPAL, principalName);
    }
}
