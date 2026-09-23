package ir.jetvam.infra.web.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds external settings for the jetvam web infrastructure.
 * Typed defaults and validation keep application configuration consistent.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@Getter
@ConfigurationProperties(prefix = "jetvam.web")
public class JetvamWebProperties {

    private final Response response = new Response();
    private final Errors errors = new Errors();

    /**
     * Configures automatic wrapping of successful controller responses.
     * Excluded paths preserve framework-specific response formats.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Getter
    @Setter
    public static class Response {
        private boolean envelopeEnabled = true;
        private List<String> excludedPaths = new ArrayList<>(List.of(
                "/actuator",
                "/v3/api-docs",
                "/swagger-ui"
        ));
    }

    /**
     * Configures the amount of diagnostic detail exposed in API errors.
     * Production deployments can suppress internal exception information.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Getter
    @Setter
    public static class Errors {
        private boolean includeDetails = true;
    }
}
