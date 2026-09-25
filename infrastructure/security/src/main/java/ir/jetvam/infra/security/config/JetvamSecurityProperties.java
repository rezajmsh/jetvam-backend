package ir.jetvam.infra.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configures the reusable JWT resource-server behavior for Jetvam applications.
 * Public endpoints and protection policy remain application-configurable.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Getter
@Setter
@ConfigurationProperties("jetvam.security.resource-server")
public class JetvamSecurityProperties {

    private boolean enabled = true;
    private List<String> publicPaths = new ArrayList<>(List.of(
            "/actuator/health/**",
            "/actuator/info",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    ));
}
