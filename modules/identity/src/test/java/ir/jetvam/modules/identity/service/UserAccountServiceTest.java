package ir.jetvam.modules.identity.service;

import ir.jetvam.common.security.UserCategory;
import ir.jetvam.modules.identity.repository.CustomerProfileRepository;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import ir.jetvam.modules.identity.repository.PartyRepository;
import ir.jetvam.modules.identity.repository.RoleRepository;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Verifies user-management validation before identity data is persisted.
 * Invalid Iranian identifiers must never reach repositories or credential storage.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
class UserAccountServiceTest {

    @Test
    void rejectsInvalidNationalCodeBeforeCreatingIdentityRecords() {
        var service = new DefaultUserAccountService(
                mock(PartyRepository.class),
                mock(IndividualPartyRepository.class),
                mock(UserAccountRepository.class),
                mock(RoleRepository.class),
                mock(CustomerProfileRepository.class),
                PasswordEncoderFactories.createDelegatingPasswordEncoder()
        );
        var command = new CreateUserCommand(
                null,
                "09121234567",
                "1111111111",
                "Reza",
                "Jamshidi",
                null,
                "strong-password",
                Set.of(UserCategory.CUSTOMER),
                Set.of()
        );

        assertThatIllegalArgumentException().isThrownBy(() -> service.create(command));
    }
}
