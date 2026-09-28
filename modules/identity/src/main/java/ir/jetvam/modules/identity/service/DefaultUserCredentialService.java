package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.exception.OperationNotAllowedException;
import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.IdentityErrorCode;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.persistence.RoleEntity;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Changes and administratively resets password credentials under a row lock.
 * Customer OTP accounts are explicitly excluded from password operations.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultUserCredentialService implements UserCredentialService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final TimeProvider timeProvider;

    @Override
    @Transactional
    public UserView changePassword(UUID userId, String currentPassword, String newPassword) {
        UserAccountEntity account = passwordAccount(userId);
        if (account.getStatus() != UserAccountStatus.ACTIVE) {
            throw new OperationNotAllowedException(
                    "change-password",
                    "Password cannot be changed while the account is not active"
            );
        }
        String current = Preconditions.requireText(currentPassword, "currentPassword");
        if (!passwordEncoder.matches(current, account.getPasswordHash())) {
            throw validation(IdentityErrorCode.CURRENT_PASSWORD_INVALID, "Current password is invalid");
        }
        replacePassword(account, newPassword);
        return toView(account);
    }

    @Override
    @Transactional
    public UserView resetPassword(UUID userId, String newPassword) {
        UserAccountEntity account = passwordAccount(userId);
        replacePassword(account, newPassword);
        return toView(account);
    }

    private UserAccountEntity passwordAccount(UUID userId) {
        Preconditions.requireNonNull(userId, "userId");
        UserAccountEntity account = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user", userId));
        if (account.getPrimaryAuthenticationMethod() != AuthenticationMethod.PASSWORD) {
            throw validation(
                    IdentityErrorCode.PASSWORD_ACCOUNT_REQUIRED,
                    "Password operations are available only for password accounts"
            );
        }
        return account;
    }

    private void replacePassword(UserAccountEntity account, String candidate) {
        String password = passwordPolicy.validate(candidate);
        if (passwordEncoder.matches(password, account.getPasswordHash())) {
            throw validation(
                    IdentityErrorCode.PASSWORD_REUSE_NOT_ALLOWED,
                    "New password must be different from the current password"
            );
        }
        account.changePassword(passwordEncoder.encode(password), timeProvider.now());
    }

    private UserView toView(UserAccountEntity account) {
        IndividualPartyEntity individual = account.getParty() instanceof IndividualPartyEntity party
                ? party
                : null;
        return new UserView(
                account.getId(),
                account.getParty().getId(),
                account.getParty().getDisplayName(),
                account.getUsername(),
                account.getAuthenticationMobile() == null && individual != null
                        ? individual.getMobile()
                        : account.getAuthenticationMobile(),
                account.getPrimaryAuthenticationMethod(),
                account.getStatus(),
                account.getCategories(),
                account.getRoles().stream().map(RoleEntity::getCode).collect(Collectors.toUnmodifiableSet())
        );
    }

    private static ValidationException validation(IdentityErrorCode code, String message) {
        return new ValidationException(code, message, List.of());
    }
}
