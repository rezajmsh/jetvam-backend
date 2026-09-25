package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.common.security.UserCategory;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.IdentityOtpPurposes;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
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
 * Unknown mobiles receive the same challenge flow to reduce account enumeration.
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
    public OtpChallengeView requestOtp(String value) {
        String mobile = IranianIdentifiers.normalizeMobileNumber(value);
        Preconditions.require(IranianIdentifiers.isValidMobileNumber(mobile), "mobile is invalid");
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
        UserAccountEntity account = individualRepository.findByMobile(verified.mobile())
                .flatMap(individual -> userRepository.findByParty_IdAndPrimaryAuthenticationMethod(
                        individual.getId(),
                        AuthenticationMethod.OTP
                ))
                .filter(candidate -> candidate.getCategories().contains(UserCategory.CUSTOMER))
                .orElseThrow(DefaultCustomerAuthenticationService::authenticationFailed);
        IdentityUserPrincipal principal = IdentityUserPrincipal.from(account);
        if (!principal.isEnabled() || !principal.isAccountNonLocked()) {
            throw authenticationFailed();
        }
        account.recordSuccessfulLogin(timeProvider.now());
        return principal;
    }

    private static ValidationException authenticationFailed() {
        return new ValidationException("OTP is invalid or the customer account is unavailable");
    }
}
