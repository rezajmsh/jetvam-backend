package ir.jetvam.infra.cache.config;

import ir.jetvam.infra.cache.CacheProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

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

    @Getter
    @Setter
    public static class Local {
        private long maximumSize = 10_000;
        private Duration expireAfterWrite = Duration.ofMinutes(10);
        private Duration expireAfterAccess;
        private boolean recordStats = true;

    }

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
