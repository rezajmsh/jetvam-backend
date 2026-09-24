package ir.jetvam.modules.integration.model;

/**
 * Defines how healthy providers are ordered for one external capability.
 * Manual-only routing requires an active, time-bounded administrator override.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public enum ProviderRoutingMode {
    PRIORITY_FAILOVER,
    ROUND_ROBIN,
    WEIGHTED,
    MANUAL_ONLY
}
