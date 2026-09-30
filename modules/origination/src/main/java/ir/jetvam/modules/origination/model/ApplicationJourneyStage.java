package ir.jetvam.modules.origination.model;

/**
 * Defines the stable, customer-visible stages of the loan origination journey.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public enum ApplicationJourneyStage {
    PLAN_SELECTION,
    ELIGIBILITY_CONTROLS,
    PERSONAL_INFORMATION,
    EMPLOYMENT_INFORMATION,
    GUARANTEE_AND_COLLATERAL,
    APPLICATION_FEES,
    ORIGINAL_COLLATERAL_DELIVERY,
    CONTRACT_SIGNATURE,
    CREDIT_ALLOCATION
}
