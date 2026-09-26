package ir.jetvam.modules.inquiry;

/**
 * Declares stable routing keys for external inquiry capabilities.
 * Integration treats these codes as opaque values and contains no inquiry semantics.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class InquiryCapabilities {

    public static final String MOBILE_OWNERSHIP = "SHAHKAR_VERIFY";
    public static final String CIVIL_REGISTRATION = "CIVIL_REGISTRATION_INQUIRY";
    public static final String MILITARY_STATUS = "MILITARY_STATUS_INQUIRY";
    public static final String BANK_ACCOUNT_STATUS = "BANK_ACCOUNT_STATUS_INQUIRY";
    public static final String BANKING_FACILITIES = "BANKING_FACILITIES_INQUIRY";
    public static final String BAD_CHEQUE = "BAD_CHEQUE_INQUIRY";
    public static final String CREDIT_RATING = "CREDIT_RATING_INQUIRY";
    public static final String CREDIT_RATING_SUBMIT = "CREDIT_RATING_SUBMIT";
    public static final String CREDIT_RATING_POLL = "CREDIT_RATING_POLL";

    private InquiryCapabilities() {
    }
}
