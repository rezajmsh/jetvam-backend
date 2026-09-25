package ir.jetvam.modules.otp.service;

import ir.jetvam.modules.otp.OtpPurpose;

import java.util.UUID;

/**
 * Defines reusable OTP issuance, validation and single-use consumption operations.
 * Validation can precede slow external I/O while consumption remains atomic with local completion.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public interface OtpChallengeService {

    OtpChallengeView issue(String mobile, String nationalCode, OtpPurpose purpose);

    OtpVerificationData verify(UUID challengeId, String code, OtpPurpose purpose);

    OtpVerificationData consume(UUID challengeId, String code, OtpPurpose purpose);

    OtpVerificationData consumeAtomically(UUID challengeId, String code, OtpPurpose purpose);
}
