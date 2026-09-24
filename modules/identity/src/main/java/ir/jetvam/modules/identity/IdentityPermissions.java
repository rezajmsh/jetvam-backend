package ir.jetvam.modules.identity;

/**
 * Stable privilege codes emitted in tokens and consumed by method security.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public final class IdentityPermissions {

    public static final String BACKOFFICE_ACCESS = "backoffice:access";
    public static final String IDENTITY_USER_READ = "identity:user:read";
    public static final String IDENTITY_USER_WRITE = "identity:user:write";
    public static final String IDENTITY_ROLE_MANAGE = "identity:role:manage";
    public static final String IDENTITY_CLIENT_MANAGE = "identity:client:manage";
    public static final String SETTINGS_READ = "settings:read";
    public static final String SETTINGS_WRITE = "settings:write";
    public static final String PROFILE_READ_SELF = "profile:read:self";
    public static final String PROFILE_WRITE_SELF = "profile:write:self";
    public static final String LOAN_REQUEST_SELF = "loan:request:self";
    public static final String MERCHANT_READ_SELF = "merchant:read:self";
    public static final String MERCHANT_WRITE_SELF = "merchant:write:self";
    public static final String MERCHANT_USER_READ_SELF = "merchant:user:read:self";
    public static final String MERCHANT_USER_WRITE_SELF = "merchant:user:write:self";

    private IdentityPermissions() {
    }
}
