package ir.jetvam.infra.security;

/**
 * Defines the stable JWT claim contract shared by the issuer and resource servers.
 * Constants prevent claim-name drift between authentication components.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public final class SecurityClaims {

    public static final String USER_ID = "user_id";
    public static final String PARTY_ID = "party_id";
    public static final String CATEGORIES = "categories";
    public static final String ROLES = "roles";
    public static final String PERMISSIONS = "permissions";

    private SecurityClaims() {
    }
}
