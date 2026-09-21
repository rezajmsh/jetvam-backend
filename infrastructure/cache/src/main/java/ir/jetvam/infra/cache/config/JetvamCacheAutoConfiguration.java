package ir.jetvam.infra.cache.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import ir.jetvam.infra.cache.JetvamCache;
import ir.jetvam.infra.cache.SpringJetvamCache;
import ir.jetvam.common.validation.Preconditions;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.cache.BatchStrategies;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;

@AutoConfiguration(before = CacheAutoConfiguration.class)
@EnableCaching
@ConditionalOnProperty(prefix = "jetvam.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(JetvamCacheProperties.class)
public class JetvamCacheAutoConfiguration {

    @Bean
    @ConditionalOnClass(Caffeine.class)
    @ConditionalOnMissingBean(CacheManager.class)
    @ConditionalOnProperty(prefix = "jetvam.cache", name = "provider", havingValue = "local", matchIfMissing = true)
    public CacheManager jetvamLocalCacheManager(JetvamCacheProperties properties) {
        JetvamCacheProperties.Local local = properties.getLocal();
        Preconditions.requirePositive(local.getMaximumSize(), "jetvam.cache.local.maximum-size");
        Caffeine<Object, Object> builder = Caffeine.newBuilder().maximumSize(local.getMaximumSize());
        if (positive(local.getExpireAfterWrite())) {
            builder.expireAfterWrite(local.getExpireAfterWrite());
        }
        if (positive(local.getExpireAfterAccess())) {
            builder.expireAfterAccess(local.getExpireAfterAccess());
        }
        if (local.isRecordStats()) {
            builder.recordStats();
        }

        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(builder);
        manager.setAllowNullValues(properties.isAllowNullValues());
        if (!properties.getNames().isEmpty()) {
            manager.setCacheNames(properties.getNames());
        }
        return manager;
    }

    @Bean(destroyMethod = "destroy")
    @ConditionalOnClass(LettuceConnectionFactory.class)
    @ConditionalOnMissingBean(LettuceConnectionFactory.class)
    @ConditionalOnProperty(prefix = "jetvam.cache", name = "provider", havingValue = "redis")
    public LettuceConnectionFactory jetvamRedisConnectionFactory(JetvamCacheProperties properties) {
        JetvamCacheProperties.Redis redis = properties.getRedis();
        Preconditions.requireText(redis.getHost(), "jetvam.cache.redis.host");
        Preconditions.requireInRange(redis.getPort(), 1, 65_535, "jetvam.cache.redis.port");
        Preconditions.requireNonNegative(redis.getDatabase(), "jetvam.cache.redis.database");
        Preconditions.requirePositive(redis.getScanBatchSize(), "jetvam.cache.redis.scan-batch-size");
        Preconditions.requireNonNull(redis.getConnectTimeout(), "jetvam.cache.redis.connect-timeout");
        Preconditions.requireNonNull(redis.getCommandTimeout(), "jetvam.cache.redis.command-timeout");
        Preconditions.requireNonNull(redis.getShutdownTimeout(), "jetvam.cache.redis.shutdown-timeout");
        Preconditions.requireNonNull(redis.getDefaultTtl(), "jetvam.cache.redis.default-ttl");
        RedisStandaloneConfiguration server = new RedisStandaloneConfiguration(redis.getHost(), redis.getPort());
        server.setDatabase(redis.getDatabase());
        if (StringUtils.hasText(redis.getUsername())) {
            server.setUsername(redis.getUsername());
        }
        if (StringUtils.hasText(redis.getPassword())) {
            server.setPassword(RedisPassword.of(redis.getPassword()));
        }

        LettuceClientConfiguration.LettuceClientConfigurationBuilder client = LettuceClientConfiguration.builder()
                .clientOptions(ClientOptions.builder()
                        .socketOptions(SocketOptions.builder()
                                .connectTimeout(redis.getConnectTimeout())
                                .build())
                        .build())
                .commandTimeout(redis.getCommandTimeout())
                .shutdownTimeout(redis.getShutdownTimeout());
        if (redis.isSsl()) {
            client.useSsl();
        }
        LettuceConnectionFactory factory = new LettuceConnectionFactory(server, client.build());
        factory.setValidateConnection(true);
        return factory;
    }

    @Bean
    @ConditionalOnClass(RedisCacheManager.class)
    @ConditionalOnMissingBean(CacheManager.class)
    @ConditionalOnProperty(prefix = "jetvam.cache", name = "provider", havingValue = "redis")
    public CacheManager jetvamRedisCacheManager(
            LettuceConnectionFactory connectionFactory,
            JetvamCacheProperties properties
    ) {
        JetvamCacheProperties.Redis redis = properties.getRedis();
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(redis.getDefaultTtl())
                .prefixCacheNameWith(redis.getKeyPrefix())
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()
                ))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new JdkSerializationRedisSerializer()
                ));
        if (!properties.isAllowNullValues()) {
            configuration = configuration.disableCachingNullValues();
        }

        RedisCacheWriter cacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(
                connectionFactory,
                BatchStrategies.scan(redis.getScanBatchSize())
        );
        RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(cacheWriter)
                .cacheDefaults(configuration);
        if (properties.getRedis().isTransactionAware()) {
            builder.transactionAware();
        }
        if (!properties.getNames().isEmpty()) {
            builder.initialCacheNames(properties.getNames());
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean(JetvamCache.class)
    public JetvamCache jetvamCache(CacheManager cacheManager) {
        return new SpringJetvamCache(cacheManager);
    }

    private static boolean positive(Duration duration) {
        return duration != null && !duration.isZero() && !duration.isNegative();
    }
}
