package ir.jetvam.modules.product.model;

/**
 * Identifies the supported eligibility policies that can be configured on a plan.
 * Product declares these policies while Assessment obtains facts and evaluates them.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public enum PlanControlType {
    AGE_RANGE,
    MINIMUM_CREDIT_RANK,
    NO_BAD_CHEQUE
}
