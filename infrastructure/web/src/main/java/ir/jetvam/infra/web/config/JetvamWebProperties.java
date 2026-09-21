package ir.jetvam.infra.web.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@ConfigurationProperties(prefix = "jetvam.web")
public class JetvamWebProperties {

    private final Response response = new Response();
    private final Errors errors = new Errors();

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

    @Getter
    @Setter
    public static class Errors {
        private boolean includeDetails = true;
    }
}
