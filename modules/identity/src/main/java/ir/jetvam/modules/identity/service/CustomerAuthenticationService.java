package ir.jetvam.modules.identity.service;

import ir.jetvam.modules.otp.service.OtpChallengeView;

import ir.jetvam.modules.identity.security.IdentityUserPrincipal;

import java.util.UUID;

/**
 * Defines passwordless customer login using a purpose-bound OTP challenge.
 * Successful verification returns the same principal used by Spring Security.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public interface CustomerAuthenticationService {

    OtpChallengeView requestOtp(String mobile);

    IdentityUserPrincipal authenticate(UUID challengeId, String otp);
}
