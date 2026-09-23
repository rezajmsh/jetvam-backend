package ir.jetvam.modules.identity.security;

import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that the password authentication provider rejects customer mobile login.
 * OTP accounts are available only through the dedicated OAuth grant.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
class JpaIdentityUserDetailsServiceTest {

    @Test
    void excludesOtpAccountsFromPasswordAuthentication() {
        UserAccountRepository userRepository = mock(UserAccountRepository.class);
        when(userRepository.findByUsernameIgnoreCaseAndPrimaryAuthenticationMethod(
                "09121234567",
                AuthenticationMethod.PASSWORD
        )).thenReturn(Optional.empty());
        var service = new JpaIdentityUserDetailsService(userRepository);

        assertThatThrownBy(() -> service.loadUserByUsername("09121234567"))
                .isInstanceOf(UsernameNotFoundException.class);

        verify(userRepository).findByUsernameIgnoreCaseAndPrimaryAuthenticationMethod(
                "09121234567",
                AuthenticationMethod.PASSWORD
        );
    }
}
