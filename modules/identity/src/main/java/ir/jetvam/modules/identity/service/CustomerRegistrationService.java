package ir.jetvam.modules.identity.service;

import ir.jetvam.modules.otp.service.OtpChallengeView;

import java.util.UUID;

/**
 * Defines customer registration and post-registration profile completion use cases.
 * Account provisioning occurs only after both OTP and Shahkar succeed.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public interface CustomerRegistrationService {

    OtpChallengeView requestOtp(StartCustomerRegistrationCommand command);

    CustomerRegistrationResult verifyOtp(VerifyCustomerRegistrationCommand command);

    CustomerProfileView completeProfile(UUID userId, CompleteCustomerProfileCommand command);
}
