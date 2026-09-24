package ir.jetvam.modules.integration.model;

/**
 * Identifies reusable authentication schemes for outbound provider calls.
 * Provider-specific signatures use CUSTOM and remain encapsulated by their adapter.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public enum ProviderAuthenticationType {
    NONE,
    API_KEY,
    BASIC,
    BEARER,
    CUSTOM
}
