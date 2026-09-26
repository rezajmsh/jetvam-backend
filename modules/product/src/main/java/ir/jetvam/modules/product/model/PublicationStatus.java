package ir.jetvam.modules.product.model;

/**
 * Represents whether a product or plan is editable, customer-visible, or retired.
 * Only active records are exposed through the customer catalog.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public enum PublicationStatus {
    DRAFT,
    ACTIVE,
    INACTIVE
}
