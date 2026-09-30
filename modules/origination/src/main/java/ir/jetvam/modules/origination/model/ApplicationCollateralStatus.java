package ir.jetvam.modules.origination.model;

/**
 * Tracks one snapshotted collateral requirement from data entry through operational acceptance.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public enum ApplicationCollateralStatus {
    WAITING_PROVIDER,
    WAITING_INFORMATION,
    WAITING_ORIGINAL_DELIVERY,
    ACCEPTED,
    REJECTED
}
