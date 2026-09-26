package ir.jetvam.modules.identity.service;

import ir.jetvam.modules.otp.service.OtpChallengeView;

/**
 * Defines customer registration use cases through OTP and mobile ownership verification.
 * Account provisioning occurs only after both OTP and Shahkar succeed.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public interface CustomerRegistrationService {

    OtpChallengeView requestOtp(StartCustomerRegistrationCommand command);

    CustomerRegistrationResult verifyOtp(VerifyCustomerRegistrationCommand command);
}
