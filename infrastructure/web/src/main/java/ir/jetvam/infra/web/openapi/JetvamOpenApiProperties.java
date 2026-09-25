package ir.jetvam.infra.web.openapi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configures shared OpenAPI metadata exposed by each Jetvam application.
 * Endpoint grouping is declared per application through native Springdoc group configuration.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Getter
@Setter
@ConfigurationProperties("jetvam.web.openapi")
public class JetvamOpenApiProperties {

    private boolean enabled = true;
    private String title = "Jetvam API";
    private String description = "Jetvam application APIs";
    private String version = "v1";
}
