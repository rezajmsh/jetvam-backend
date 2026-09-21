package ir.jetvam.infra.web.config;

import ir.jetvam.infra.core.config.JetvamCoreAutoConfiguration;
import ir.jetvam.infra.observability.config.JetvamObservabilityAutoConfiguration;
import ir.jetvam.infra.web.api.ApiResponseFactory;
import ir.jetvam.infra.web.api.JetvamResponseBodyAdvice;
import ir.jetvam.infra.web.error.ExceptionHttpStatusMapper;
import ir.jetvam.infra.web.error.GlobalExceptionHandler;
import ir.jetvam.infra.web.observability.HttpClientObservabilityInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JetvamWebAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    JetvamCoreAutoConfiguration.class,
                    JetvamObservabilityAutoConfiguration.class,
                    JetvamWebAutoConfiguration.class
            ));

    @Test
    void createsResponseErrorAndHttpObservabilityInfrastructure() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ApiResponseFactory.class);
            assertThat(context).hasSingleBean(JetvamResponseBodyAdvice.class);
            assertThat(context).hasSingleBean(ExceptionHttpStatusMapper.class);
            assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
            assertThat(context).hasSingleBean(JetvamWebProperties.class);
        });
    }

    @Test
    void keepsApiContractButDisablesHttpInstrumentationWhenObservabilityIsOff() {
        contextRunner
                .withPropertyValues("jetvam.observability.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(ApiResponseFactory.class);
                    assertThat(context).hasSingleBean(GlobalExceptionHandler.class);
                    assertThat(context).doesNotHaveBean(HttpClientObservabilityInterceptor.class);
                    assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);
                });
    }
}
