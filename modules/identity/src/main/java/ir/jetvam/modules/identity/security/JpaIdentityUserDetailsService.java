package ir.jetvam.modules.identity.security;

import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Authenticates password-based operators and merchant users by username.
 * Customer OTP accounts are deliberately excluded from the password login provider.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Service
@RequiredArgsConstructor
public class JpaIdentityUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        String username = login.strip().toLowerCase(Locale.ROOT);
        return userAccountRepository.findByUsernameIgnoreCaseAndPrimaryAuthenticationMethod(
                        username,
                        AuthenticationMethod.PASSWORD
                )
                .map(IdentityUserPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("User account was not found"));
    }
}
