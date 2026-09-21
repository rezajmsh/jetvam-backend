package ir.jetvam.infra.i18n.config;

import ir.jetvam.infra.cache.config.JetvamCacheAutoConfiguration;
import ir.jetvam.infra.i18n.MessageRepository;
import ir.jetvam.infra.i18n.MessageResolver;
import ir.jetvam.infra.persistence.config.JetvamPersistenceAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;

import static org.assertj.core.api.Assertions.assertThat;

class JetvamI18nAutoConfigurationTest {

    @Test
    void assemblesDatabaseBackedSpringMessageSourceWithoutApplicationBeans() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JetvamPersistenceAutoConfiguration.class,
                        JetvamCacheAutoConfiguration.class,
                        JetvamI18nAutoConfiguration.class,
                        MessageSourceAutoConfiguration.class
                ))
                .withPropertyValues(
                        "jetvam.persist.pool.initialization-fail-timeout=-1",
                        "jetvam.persist.migration.enabled=false",
                        "jetvam.cache.names[0]=i18n-messages",
                        "jetvam.i18n.default-locale=fa-IR"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(MessageRepository.class);
                    assertThat(context).hasSingleBean(MessageResolver.class);
                    assertThat(context).hasBean("messageSource");
                    assertThat(context.getBean("messageSource")).isInstanceOf(MessageSource.class);
                });
    }
}
