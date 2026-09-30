package ir.jetvam.modules.origination.model;

/**
 * Describes progress of a customer-visible application stage independently from technical statuses.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public enum ApplicationJourneyStageStatus {
    COMPLETED,
    CURRENT,
    UPCOMING,
    FAILED,
    CANCELLED
}
