package ir.jetvam.infra.i18n.config;

import ir.jetvam.infra.cache.config.JetvamCacheAutoConfiguration;
import ir.jetvam.infra.i18n.MessageRepository;
import ir.jetvam.infra.i18n.MessageResolver;
import ir.jetvam.infra.i18n.persistence.I18nMessageJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies the behavior of jetvam i18n auto configuration.
 * The tests protect the shared contract and its important edge cases.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

class JetvamI18nAutoConfigurationTest {

    @Test
    void assemblesDatabaseBackedSpringMessageSourceWithoutApplicationBeans() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        JetvamCacheAutoConfiguration.class,
                        JetvamI18nAutoConfiguration.class,
                        MessageSourceAutoConfiguration.class
                ))
                .withBean(I18nMessageJpaRepository.class, () -> mock(I18nMessageJpaRepository.class))
                .withPropertyValues(
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
