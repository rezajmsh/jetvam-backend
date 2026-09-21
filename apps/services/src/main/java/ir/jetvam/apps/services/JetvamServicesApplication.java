package ir.jetvam.apps.services;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ir.jetvam.apps.services", "ir.jetvam.modules"})
public class JetvamServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(JetvamServicesApplication.class, args);
    }
}
