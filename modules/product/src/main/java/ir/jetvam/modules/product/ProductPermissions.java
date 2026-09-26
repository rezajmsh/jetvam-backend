package ir.jetvam.modules.product;

/**
 * Declares stable permissions for product catalog access and plan configuration.
 * Controllers combine these granular authorities with the appropriate user roles.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class ProductPermissions {

    public static final String CATALOG_READ = "product:catalog:read";
    public static final String CONFIGURATION_READ = "product:configuration:read";
    public static final String CONFIGURATION_WRITE = "product:configuration:write";

    private ProductPermissions() {
    }
}
