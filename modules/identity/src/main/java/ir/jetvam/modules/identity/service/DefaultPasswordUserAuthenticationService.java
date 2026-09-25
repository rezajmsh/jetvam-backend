package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.common.security.UserCategory;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.IdentityOtpPurposes;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.otp.service.OtpChallengeService;
import ir.jetvam.modules.otp.service.OtpChallengeView;
import ir.jetvam.modules.otp.service.OtpVerificationData;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import ir.jetvam.modules.identity.security.IdentityUserPrincipal;
import ir.jetvam.modules.settings.SettingKeys;
import ir.jetvam.modules.settings.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Applies category-specific 2FA settings without allowing password login for customers.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultPasswordUserAuthenticationService implements PasswordUserAuthenticationService {

    private final UserAccountRepository userRepository;
    private final OtpChallengeService otpChallengeService;
    private final SettingService settingService;
    private final PasswordEncoder passwordEncoder;
    private final TimeProvider timeProvider;

    @Override
    @Transactional
    public PasswordAuthenticationResult authenticate(
            String username,
            String password,
            UUID challengeId,
            String otp
    ) {
        UserAccountEntity account = passwordAccount(username, password);
        if (requiresSecondFactor(account)) {
            if (challengeId == null && (otp == null || otp.isBlank())) {
                OtpChallengeView challenge = otpChallengeService.issue(
                        verifiedMobile(account),
                        null,
                        IdentityOtpPurposes.PASSWORD_LOGIN_SECOND_FACTOR
                );
                return PasswordAuthenticationResult.secondFactorRequired(challenge);
            }
            Preconditions.requireNonNull(challengeId, "challengeId");
            Preconditions.requireText(otp, "otp");
            OtpVerificationData verified = otpChallengeService.consume(
                    challengeId,
                    otp,
                    IdentityOtpPurposes.PASSWORD_LOGIN_SECOND_FACTOR
            );
            if (!verifiedMobile(account).equals(verified.mobile())) {
                throw authenticationFailed();
            }
        } else if (challengeId != null || (otp != null && !otp.isBlank())) {
            throw authenticationFailed();
        }
        account.recordSuccessfulLogin(timeProvider.now());
        return PasswordAuthenticationResult.authenticated(IdentityUserPrincipal.from(account));
    }

    private UserAccountEntity passwordAccount(String value, String password) {
        String username = Preconditions.requireText(value, "username").strip().toLowerCase(Locale.ROOT);
        String credential = Preconditions.requireText(password, "password");
        UserAccountEntity account = userRepository.findByUsernameIgnoreCaseAndPrimaryAuthenticationMethod(
                        username,
                        AuthenticationMethod.PASSWORD
                )
                .orElseThrow(DefaultPasswordUserAuthenticationService::authenticationFailed);
        IdentityUserPrincipal principal = IdentityUserPrincipal.from(account);
        if (!principal.isEnabled() || !principal.isAccountNonLocked()
                || !passwordEncoder.matches(credential, account.getPasswordHash())
                || account.getCategories().contains(UserCategory.CUSTOMER)) {
            throw authenticationFailed();
        }
        return account;
    }

    private boolean requiresSecondFactor(UserAccountEntity account) {
        boolean systemRequired = account.getCategories().contains(UserCategory.OPERATOR)
                && settingService.getBoolean(SettingKeys.SYSTEM_USER_TWO_FACTOR_REQUIRED);
        boolean merchantRequired = account.getCategories().contains(UserCategory.MERCHANT)
                && settingService.getBoolean(SettingKeys.MERCHANT_USER_TWO_FACTOR_REQUIRED);
        return systemRequired || merchantRequired;
    }

    private String verifiedMobile(UserAccountEntity account) {
        String mobile = account.getAuthenticationMobile();
        if (mobile == null || mobile.isBlank()) {
            throw new ValidationException("A mobile number is required for two-factor authentication");
        }
        return mobile;
    }

    private static ValidationException authenticationFailed() {
        return new ValidationException("Username, password or second factor is invalid");
    }
}
