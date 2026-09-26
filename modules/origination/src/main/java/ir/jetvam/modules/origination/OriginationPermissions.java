package ir.jetvam.modules.origination;

/**
 * Declares customer and operational permissions for loan origination.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class OriginationPermissions {

    public static final String SELF_READ = "origination:self:read";
    public static final String SELF_WRITE = "origination:self:write";
    public static final String MANAGE = "origination:manage";
    public static final String PROCESS = "origination:process";

    private OriginationPermissions() {
    }
}
