package ir.jetvam.modules.identity.security;

import ir.jetvam.common.security.AuthenticatedUser;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import ir.jetvam.modules.identity.repository.UserAccountSecurityState;
import ir.jetvam.infra.security.SecurityClaims;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies immediate invalidation of JWTs after account security changes.
 * Active tokens issued after the last change remain valid.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
class ActiveUserAccountValidatorTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final Instant ISSUED_AT = Instant.parse("2026-09-28T03:00:00Z");

    @Test
    void rejectsTokenForDisabledAccount() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        when(repository.findSecurityStateById(USER_ID))
                .thenReturn(Optional.of(state(UserAccountStatus.DISABLED, 0)));

        assertThatThrownBy(() -> new ActiveUserAccountValidator(repository).validate(user(), ISSUED_AT))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void rejectsTokenWithStaleAuthenticationVersion() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        when(repository.findSecurityStateById(USER_ID)).thenReturn(Optional.of(state(
                UserAccountStatus.ACTIVE,
                2
        )));

        assertThatThrownBy(() -> new ActiveUserAccountValidator(repository).validate(user(), ISSUED_AT))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void acceptsActiveTokenWithCurrentAuthenticationVersion() {
        UserAccountRepository repository = mock(UserAccountRepository.class);
        when(repository.findSecurityStateById(USER_ID)).thenReturn(Optional.of(state(
                UserAccountStatus.ACTIVE,
                1
        )));

        assertThatCode(() -> new ActiveUserAccountValidator(repository).validate(user(), ISSUED_AT))
                .doesNotThrowAnyException();
    }

    private static AuthenticatedUser user() {
        return new AuthenticatedUser(
                USER_ID,
                UUID.randomUUID(),
                "user",
                Set.of(),
                Set.of(),
                Set.of(),
                Map.of(SecurityClaims.AUTHENTICATION_VERSION, 1L)
        );
    }

    private static UserAccountSecurityState state(UserAccountStatus status, long version) {
        return new UserAccountSecurityState() {
            @Override
            public UserAccountStatus getStatus() {
                return status;
            }

            @Override
            public Instant getAuthenticationChangedAt() {
                return null;
            }

            @Override
            public long getAuthenticationVersion() {
                return version;
            }
        };
    }
}
