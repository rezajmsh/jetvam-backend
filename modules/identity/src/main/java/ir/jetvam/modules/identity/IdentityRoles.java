package ir.jetvam.modules.identity;

/**
 * Defines built-in role codes used during account provisioning and authorization.
 * Additional operational roles can still be introduced as database data.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public final class IdentityRoles {

    public static final String CUSTOMER = "CUSTOMER";
    public static final String MERCHANT_ADMIN = "MERCHANT_ADMIN";
    public static final String MERCHANT_OPERATOR = "MERCHANT_OPERATOR";
    public static final String MERCHANT_USER = "MERCHANT_USER";
    public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";
    public static final String SYSTEM_OPERATOR = "SYSTEM_OPERATOR";
    /** @deprecated use {@link #SYSTEM_ADMIN}; retained for existing assignments. */
    @Deprecated
    public static final String UAA_ADMIN = "UAA_ADMIN";
    public static final String SERVICE = "SERVICE";

    private IdentityRoles() {
    }
}
