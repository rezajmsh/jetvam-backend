package ir.jetvam.modules.origination.model;

/**
 * Represents the customer-visible progression and terminal outcomes of a loan application.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public enum ApplicationStatus {
    WAITING_CONTROL_FEE,
    WAITING_CONTROLS,
    WAITING_PERSONAL_INFORMATION,
    WAITING_EMPLOYMENT_INFORMATION,
    WAITING_GUARANTEE,
    WAITING_APPLICATION_FEE,
    WAITING_ORIGINAL_CHEQUE,
    WAITING_SIGNATURE,
    WAITING_CREDIT_ALLOCATION,
    MANUAL_REVIEW,
    REJECTED,
    COMPLETED,
    CANCELLED
}
