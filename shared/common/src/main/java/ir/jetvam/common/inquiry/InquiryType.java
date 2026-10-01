package ir.jetvam.common.inquiry;

import java.util.Arrays;

/**
 * Defines the stable inquiry codes shared by product configuration, workflow snapshots and execution.
 * The code is persisted and exposed at system boundaries while the enum remains type-safe in domain models.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public enum InquiryType {

    MOBILE_OWNERSHIP("SHAHKAR_VERIFY"),
    CIVIL_REGISTRATION("CIVIL_REGISTRATION_INQUIRY"),
    MILITARY_STATUS("MILITARY_STATUS_INQUIRY"),
    BANK_ACCOUNT_STATUS("BANK_ACCOUNT_STATUS_INQUIRY"),
    BANKING_FACILITIES("BANKING_FACILITIES_INQUIRY"),
    BAD_CHEQUE("BAD_CHEQUE_INQUIRY"),
    CREDIT_RATING("CREDIT_RATING_INQUIRY"),
    CREDIT_RATING_SUBMIT("CREDIT_RATING_SUBMIT"),
    CREDIT_RATING_POLL("CREDIT_RATING_POLL");

    private final String code;

    InquiryType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static InquiryType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("inquiryCode must not be blank");
        }
        String normalized = code.strip();
        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported inquiry code: " + code));
    }

}
