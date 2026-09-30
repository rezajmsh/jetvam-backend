package ir.jetvam.apps.services;

import ir.jetvam.modules.otp.service.OtpChallengeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies reusable OTP wiring and the Services application's HTTP security boundary.
 * Infrastructure endpoints stay public while business APIs require authentication.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jetvam.persist.url=jdbc:h2:mem:jetvam_services_context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "jetvam.persist.driver-class-name=org.h2.Driver",
                "jetvam.persist.username=sa",
                "jetvam.persist.password=",
                "jetvam.persist.default-schema=PUBLIC",
                "jetvam.persist.jpa.ddl-auto=create-drop",
                "jetvam.persist.migration.enabled=false",
                "jetvam.notification.dispatcher.enabled=false",
                "jetvam.i18n.enabled=false",
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/unused"
        }
)
class JetvamServicesApplicationContextTest {

    @Autowired
    private ApplicationContext context;

    @Value("${local.server.port}")
    private int port;

    @Test
    void exposesReusableOtpCapability() {
        assertThat(context.getBean(OtpChallengeService.class)).isNotNull();
    }

    @Test
    void exposesInfrastructureEndpointsButProtectsBusinessApis() throws IOException, InterruptedException {
        assertThat(get("/swagger-ui/index.html").statusCode()).isNotIn(401, 403);
        assertThat(get("/actuator").statusCode()).isNotIn(401, 403);
        assertThat(get("/actuator/health").statusCode()).isNotIn(401, 403);
        assertThat(get("/actuator/prometheus").statusCode()).isNotIn(401, 403);
        assertThat(get("/api/v1/catalog/products").statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
