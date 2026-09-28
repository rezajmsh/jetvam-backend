package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.common.security.UserCategory;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.modules.identity.IdentityErrorCode;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies password changes, reset rules and exclusion of OTP-only accounts.
 * Tests also ensure raw passwords are replaced only after current-password checks.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
class DefaultUserCredentialServiceTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000901");
    private static final UUID PARTY_ID = UUID.fromString("00000000-0000-0000-0000-000000000902");
    private static final Instant NOW = Instant.parse("2026-09-28T03:30:00Z");

    private UserAccountRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private TimeProvider timeProvider;
    private DefaultUserCredentialService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserAccountRepository.class);
        passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(NOW);
        service = new DefaultUserCredentialService(
                userRepository,
                passwordEncoder,
                new DefaultPasswordPolicy(),
                timeProvider
        );
    }

    @Test
    void changesPasswordAfterVerifyingCurrentCredential() {
        UserAccountEntity account = passwordAccount("old-password");
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(account));

        service.changePassword(USER_ID, "old-password", "new-password");

        assertThat(passwordEncoder.matches("new-password", account.getPasswordHash())).isTrue();
        assertThat(account.getAuthenticationChangedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsAnInvalidCurrentPasswordWithoutChangingCredential() {
        UserAccountEntity account = passwordAccount("old-password");
        String originalHash = account.getPasswordHash();
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.changePassword(USER_ID, "wrong-password", "new-password"))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo(IdentityErrorCode.CURRENT_PASSWORD_INVALID.code()));
        assertThat(account.getPasswordHash()).isEqualTo(originalHash);
    }

    @Test
    void rejectsPasswordOperationsForOtpAccounts() {
        UserAccountEntity account = mock(UserAccountEntity.class);
        when(account.getPrimaryAuthenticationMethod()).thenReturn(AuthenticationMethod.OTP);
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.resetPassword(USER_ID, "new-password"))
                .isInstanceOfSatisfying(ValidationException.class, exception ->
                        assertThat(exception.code()).isEqualTo(IdentityErrorCode.PASSWORD_ACCOUNT_REQUIRED.code()));
        verify(account).getPrimaryAuthenticationMethod();
    }

    private UserAccountEntity passwordAccount(String password) {
        IndividualPartyEntity party = new IndividualPartyEntity(
                "System Operator",
                "0067749828",
                "System",
                "Operator",
                null,
                "09121234567"
        );
        ReflectionTestUtils.setField(party, "id", PARTY_ID);
        UserAccountEntity account = new UserAccountEntity(
                party,
                "operator",
                passwordEncoder.encode(password),
                "09121234567",
                Set.of(UserCategory.OPERATOR),
                Set.of()
        );
        ReflectionTestUtils.setField(account, "id", USER_ID);
        return account;
    }

}
