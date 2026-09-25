package ir.jetvam.modules.otp;

import ir.jetvam.common.validation.Preconditions;

/**
 * Describes a consumer-owned OTP purpose and the notification template used when issuing it.
 * Arbitrary modules can introduce purposes without changing OTP persistence or migrations.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public record OtpPurpose(String code, String notificationTemplateCode) {

    public OtpPurpose {
        code = Preconditions.requireText(code, "purpose.code").strip();
        notificationTemplateCode = Preconditions.requireText(
                notificationTemplateCode,
                "purpose.notificationTemplateCode"
        ).strip();
    }
}
