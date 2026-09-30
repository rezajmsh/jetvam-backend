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

    MOBILE_OWNERSHIP("SHAHKAR_VERIFY", false),
    CIVIL_REGISTRATION("CIVIL_REGISTRATION_INQUIRY", true),
    MILITARY_STATUS("MILITARY_STATUS_INQUIRY", true),
    BANK_ACCOUNT_STATUS("BANK_ACCOUNT_STATUS_INQUIRY", true),
    BANKING_FACILITIES("BANKING_FACILITIES_INQUIRY", true),
    BAD_CHEQUE("BAD_CHEQUE_INQUIRY", true),
    CREDIT_RATING("CREDIT_RATING_INQUIRY", true),
    CREDIT_RATING_SUBMIT("CREDIT_RATING_SUBMIT", false),
    CREDIT_RATING_POLL("CREDIT_RATING_POLL", false);

    private final String code;
    private final boolean deferredBusinessInquiry;

    InquiryType(String code, boolean deferredBusinessInquiry) {
        this.code = code;
        this.deferredBusinessInquiry = deferredBusinessInquiry;
    }

    public String code() {
        return code;
    }

    public boolean deferredBusinessInquiry() {
        return deferredBusinessInquiry;
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

    public static InquiryType deferredFromCode(String code) {
        return requireDeferred(fromCode(code));
    }

    public static InquiryType requireDeferred(InquiryType type) {
        if (type == null || !type.deferredBusinessInquiry) {
            throw new IllegalArgumentException("Inquiry is not executable by the deferred worker: " + type);
        }
        return type;
    }
}
