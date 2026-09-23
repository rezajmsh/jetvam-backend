package ir.jetvam.modules.identity.service;

import ir.jetvam.modules.identity.model.OtpPurpose;

import java.util.UUID;

/**
 * Defines secure OTP issuance and single-use verification operations.
 * Purpose binding prevents a registration code from being replayed for login.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
public interface OtpChallengeService {

    OtpChallengeView issue(String mobile, String nationalCode, OtpPurpose purpose);

    OtpVerificationData consume(UUID challengeId, String code, OtpPurpose purpose);
}
