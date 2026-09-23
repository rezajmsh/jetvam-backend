package ir.jetvam.infra.cache;

/**
 * Enumerates the supported local and Redis cache backends.
 * Configuration selects the provider without changing callers.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public enum CacheProvider {
    LOCAL,
    REDIS
}
