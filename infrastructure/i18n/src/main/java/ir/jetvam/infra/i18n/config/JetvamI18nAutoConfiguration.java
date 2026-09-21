package ir.jetvam.infra.i18n.config;

import ir.jetvam.infra.cache.JetvamCache;
import ir.jetvam.infra.cache.config.JetvamCacheAutoConfiguration;
import ir.jetvam.infra.i18n.DatabaseMessageResolver;
import ir.jetvam.infra.i18n.DatabaseMessageSource;
import ir.jetvam.infra.i18n.JdbcMessageRepository;
import ir.jetvam.infra.i18n.MessageRepository;
import ir.jetvam.infra.i18n.MessageResolver;
import ir.jetvam.infra.persistence.config.JetvamPersistenceAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.AbstractApplicationContext;

import javax.sql.DataSource;

@AutoConfiguration(
        after = {JetvamPersistenceAutoConfiguration.class, JetvamCacheAutoConfiguration.class},
        before = MessageSourceAutoConfiguration.class
)
@ConditionalOnProperty(prefix = "jetvam.i18n", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean({DataSource.class, JetvamCache.class})
@EnableConfigurationProperties(JetvamI18nProperties.class)
public class JetvamI18nAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(MessageRepository.class)
    public MessageRepository jetvamMessageRepository(DataSource dataSource, JetvamI18nProperties properties) {
        return new JdbcMessageRepository(dataSource, properties.getTableName());
    }

    @Bean
    @ConditionalOnMissingBean(MessageResolver.class)
    public MessageResolver jetvamMessageResolver(
            MessageRepository repository,
            JetvamCache cache,
            JetvamI18nProperties properties
    ) {
        return new DatabaseMessageResolver(
                repository,
                cache,
                properties.getCacheName(),
                properties.getDefaultLocale(),
                properties.isFallbackToLanguage(),
                properties.isFallbackToDefaultLocale()
        );
    }

    @Bean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME)
    @ConditionalOnMissingBean(name = AbstractApplicationContext.MESSAGE_SOURCE_BEAN_NAME)
    public MessageSource jetvamMessageSource(MessageResolver resolver, JetvamI18nProperties properties) {
        return new DatabaseMessageSource(resolver, properties.isUseCodeAsDefaultMessage());
    }
}
