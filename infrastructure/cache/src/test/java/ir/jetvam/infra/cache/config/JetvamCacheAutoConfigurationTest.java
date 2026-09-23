package ir.jetvam.infra.cache.config;

import ir.jetvam.infra.cache.JetvamCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the behavior of jetvam cache auto configuration.
 * The tests protect the shared contract and its important edge cases.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

class JetvamCacheAutoConfigurationTest {

    @Test
    void configuresLocalCacheAndManualFacadeByDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JetvamCacheAutoConfiguration.class,
                        CacheAutoConfiguration.class
                ))
                .withPropertyValues(
                        "jetvam.cache.names[0]=i18n-messages",
                        "jetvam.cache.local.maximum-size=123",
                        "jetvam.cache.local.expire-after-write=5m"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(CacheManager.class);
                    assertThat(context.getBean(CacheManager.class)).isInstanceOf(CaffeineCacheManager.class);
                    assertThat(context).hasSingleBean(JetvamCache.class);

                    JetvamCache cache = context.getBean(JetvamCache.class);
                    cache.put("i18n-messages", "fa:test", "پیام");
                    assertThat(cache.get("i18n-messages", "fa:test", String.class)).contains("پیام");
                });
    }

    @Test
    void configuresRedisWithoutConnectingDuringContextCreation() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JetvamCacheAutoConfiguration.class))
                .withPropertyValues(
                        "jetvam.cache.provider=redis",
                        "jetvam.cache.redis.host=redis.internal",
                        "jetvam.cache.redis.port=6380",
                        "jetvam.cache.redis.database=2",
                        "jetvam.cache.redis.scan-batch-size=250"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(CacheManager.class))
                            .isInstanceOf(org.springframework.data.redis.cache.RedisCacheManager.class);
                    assertThat(context).hasSingleBean(
                            org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory.class
                    );
                });
    }
}
