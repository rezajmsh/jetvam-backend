package ir.jetvam.modules.origination.model;

/**
 * Tracks fulfillment and acceptance of one guarantor position in an application.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public enum ApplicationGuarantorStatus {
    WAITING_IDENTIFICATION,
    WAITING_ACCEPTANCE,
    ACCEPTED,
    REJECTED
}
