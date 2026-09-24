package ir.jetvam.modules.integration;

/**
 * Defines granular authorities for external-provider configuration and operations.
 * Administrative endpoints always combine these permissions with a system role.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public final class IntegrationPermissions {

    public static final String PROVIDER_READ = "integration:provider:read";
    public static final String PROVIDER_WRITE = "integration:provider:write";
    public static final String PROVIDER_OVERRIDE = "integration:provider:override";

    private IntegrationPermissions() {
    }
}
