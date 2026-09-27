package ir.jetvam.modules.identity.service;

import ir.jetvam.common.security.UserCategory;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.IdentityOtpPurposes;
import ir.jetvam.modules.identity.exception.CustomerAccountUnavailableException;
import ir.jetvam.modules.identity.exception.CustomerRegistrationRequiredException;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.modules.otp.service.OtpChallengeService;
import ir.jetvam.modules.otp.service.OtpChallengeView;
import ir.jetvam.modules.otp.service.OtpVerificationData;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import ir.jetvam.modules.identity.security.IdentityUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Authenticates customer accounts exclusively through a single-use mobile OTP.
 * Login challenges are issued only for registered and currently available customer accounts.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultCustomerAuthenticationService implements CustomerAuthenticationService {

    private final OtpChallengeService otpChallengeService;
    private final IndividualPartyRepository individualRepository;
    private final UserAccountRepository userRepository;
    private final TimeProvider timeProvider;

    @Override
    @Transactional
    public OtpChallengeView requestOtp(String value) {
        String mobile = IranianIdentifiers.normalizeMobileNumber(value);
        Preconditions.require(IranianIdentifiers.isValidMobileNumber(mobile), "mobile is invalid");
        requireAvailable(requireCustomerAccount(mobile));
        return otpChallengeService.issue(mobile, null, IdentityOtpPurposes.CUSTOMER_LOGIN);
    }

    @Override
    @Transactional
    public IdentityUserPrincipal authenticate(UUID challengeId, String otp) {
        OtpVerificationData verified = otpChallengeService.consume(
                challengeId,
                otp,
                IdentityOtpPurposes.CUSTOMER_LOGIN
        );
        UserAccountEntity account = requireCustomerAccount(verified.mobile());
        requireAvailable(account);
        IdentityUserPrincipal principal = IdentityUserPrincipal.from(account);
        account.recordSuccessfulLogin(timeProvider.now());
        return principal;
    }

    private UserAccountEntity requireCustomerAccount(String mobile) {
        return individualRepository.findByMobile(mobile)
                .flatMap(individual -> userRepository.findByParty_IdAndPrimaryAuthenticationMethod(
                        individual.getId(),
                        AuthenticationMethod.OTP
                ))
                .filter(candidate -> candidate.getCategories().contains(UserCategory.CUSTOMER))
                .orElseThrow(CustomerRegistrationRequiredException::new);
    }

    private static void requireAvailable(UserAccountEntity account) {
        if (account.getStatus() != UserAccountStatus.ACTIVE) {
            throw new CustomerAccountUnavailableException();
        }
    }
}
