package ir.jetvam.apps.uaa.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds UAA bootstrap administration and JWT signing-key settings.
 * OAuth clients are managed as persistent database records instead of properties.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Getter
@Setter
@ConfigurationProperties("jetvam.uaa")
public class JetvamUaaProperties {

    private final BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();
    private final SigningKey signingKey = new SigningKey();
    private final Providers providers = new Providers();

    /**
     * Defines the optional first administrator created on an empty installation.
     * Deployment secrets are supplied externally and are never persisted in config files.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Getter
    @Setter
    public static class BootstrapAdmin {
        private boolean enabled;
        private String username;
        private String mobile;
        private String nationalCode;
        private String firstName;
        private String lastName;
        private String password;
    }

    /**
     * Configures the persistent RSA signing key used for JWT issuance.
     * Ephemeral keys are intended only for local development and automated tests.
     *
     * @author reza jamshidi
     * @since 9/21/2026
     */
    @Getter
    @Setter
    public static class SigningKey {
        private String keyStoreLocation;
        private String keyStoreType = "PKCS12";
        private String keyStorePassword;
        private String keyAlias;
        private String keyPassword;
        private boolean allowEphemeral = true;
    }

    /**
     * Groups external identity-verification provider settings owned by UAA.
     * Message delivery is configured centrally by the notification module.
     *
     * @author reza jamshidi
     * @since 9/22/2026
     */
    @Getter
    @Setter
    public static class Providers {
        private final Endpoint shahkar = new Endpoint();
    }

    /**
     * Describes a secured HTTP provider endpoint used during customer onboarding.
     * Secrets are supplied from deployment configuration rather than source files.
     *
     * @author reza jamshidi
     * @since 9/22/2026
     */
    @Getter
    @Setter
    public static class Endpoint {
        private boolean enabled;
        private String baseUrl;
        private String path;
        private String apiKeyHeader = "X-Api-Key";
        private String apiKey;
    }
}
