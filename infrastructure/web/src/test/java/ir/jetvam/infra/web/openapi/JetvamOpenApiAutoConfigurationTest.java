package ir.jetvam.infra.web.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies shared OpenAPI metadata and the ability to disable documentation centrally.
 * Module grouping itself is bound from each hosting application's Springdoc configuration.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
class JetvamOpenApiAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JetvamOpenApiAutoConfiguration.class));

    @Test
    void createsApplicationMetadataAndBearerScheme() {
        contextRunner
                .withPropertyValues(
                        "jetvam.web.openapi.title=Test API",
                        "jetvam.web.openapi.version=v-test"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(OpenAPI.class);
                    OpenAPI openApi = context.getBean(OpenAPI.class);
                    assertThat(openApi.getInfo().getTitle()).isEqualTo("Test API");
                    assertThat(openApi.getInfo().getVersion()).isEqualTo("v-test");
                    assertThat(openApi.getComponents().getSecuritySchemes())
                            .containsKey(JetvamOpenApiAutoConfiguration.BEARER_SCHEME);
                });
    }

    @Test
    void canDisableOpenApiInfrastructure() {
        contextRunner
                .withPropertyValues("jetvam.web.openapi.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(OpenAPI.class));
    }
}
