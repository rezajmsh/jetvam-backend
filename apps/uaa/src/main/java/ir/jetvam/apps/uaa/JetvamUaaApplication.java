package ir.jetvam.apps.uaa;

import ir.jetvam.apps.uaa.config.JetvamUaaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Bootstraps the Jetvam identity and authorization-server application.
 * It hosts user management, login and OAuth endpoints.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@SpringBootApplication(scanBasePackages = {
        "ir.jetvam.apps.uaa",
        "ir.jetvam.modules.identity",
        "ir.jetvam.modules.otp",
        "ir.jetvam.modules.settings",
        "ir.jetvam.modules.notification",
        "ir.jetvam.modules.integration",
        "ir.jetvam.modules.inquiry"
})
@EnableConfigurationProperties(JetvamUaaProperties.class)
public class JetvamUaaApplication {

    public static void main(String[] args) {
        SpringApplication.run(JetvamUaaApplication.class, args);
    }
}
