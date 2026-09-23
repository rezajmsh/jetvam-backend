package ir.jetvam.infra.cache.config;

import ir.jetvam.infra.cache.CacheProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Binds external settings for the jetvam cache infrastructure.
 * Typed defaults and validation keep application configuration consistent.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@ConfigurationProperties("jetvam.cache")
@Getter
@Setter
public class JetvamCacheProperties {

    private boolean enabled = true;
    private CacheProvider provider = CacheProvider.LOCAL;
    private Set<String> names = new LinkedHashSet<>();
    private boolean allowNullValues;
    private final Local local = new Local();
    private final Redis redis = new Redis();

    /**
     * Configures bounded in-process cache size, expiry and statistics.
     * These settings are used when the local provider is selected.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Getter
    @Setter
    public static class Local {
        private long maximumSize = 10_000;
        private Duration expireAfterWrite = Duration.ofMinutes(10);
        private Duration expireAfterAccess;
        private boolean recordStats = true;

    }

    /**
     * Configures Redis connectivity, key namespacing and entry lifetime.
     * Remote cache behavior remains transparent to cache consumers.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Getter
    @Setter
    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private String username;
        private String password;
        private int database;
        private boolean ssl;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration commandTimeout = Duration.ofSeconds(3);
        private Duration shutdownTimeout = Duration.ofMillis(100);
        private Duration defaultTtl = Duration.ofMinutes(10);
        private String keyPrefix = "jetvam::";
        private int scanBatchSize = 1_000;
        private boolean transactionAware;

    }
}
