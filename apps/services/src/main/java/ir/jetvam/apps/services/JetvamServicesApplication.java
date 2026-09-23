package ir.jetvam.apps.services;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootstraps the main Jetvam business-services application.
 * It composes the modular business and shared infrastructure components.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@SpringBootApplication(scanBasePackages = {"ir.jetvam.apps.services", "ir.jetvam.modules"})
public class JetvamServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(JetvamServicesApplication.class, args);
    }
}
