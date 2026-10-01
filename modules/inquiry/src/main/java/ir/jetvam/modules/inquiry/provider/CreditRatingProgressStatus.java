package ir.jetvam.modules.inquiry.provider;

/**
 * Provider protocol state for multi-step credit-rating submission and polling.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
public enum CreditRatingProgressStatus {
    PENDING,
    COMPLETED,
    REJECTED
}
