package ir.jetvam.modules.origination.model;

/**
 * Represents the persisted lifecycle of one snapshotted application eligibility control.
 * Inquiry-backed controls use the intermediate inquiry states while local controls are evaluated immediately.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public enum ApplicationControlStatus {
    WAITING_PRIORITY,
    BLOCKED_BY_PAYMENT,
    PENDING_INQUIRY,
    INQUIRY_SUBMITTED,
    PASSED,
    FAILED,
    ERROR,
    CANCELLED
}
