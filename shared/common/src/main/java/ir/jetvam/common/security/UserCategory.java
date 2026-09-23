package ir.jetvam.common.security;

/**
 * Identifies the business contexts in which an authenticated account can act.
 * An account may belong to more than one category without duplicating its identity.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public enum UserCategory {
    CUSTOMER,
    MERCHANT,
    OPERATOR,
    SERVICE
}
