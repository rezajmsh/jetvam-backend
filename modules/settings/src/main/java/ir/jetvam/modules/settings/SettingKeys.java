package ir.jetvam.modules.settings;

/**
 * Stable keys for settings whose meaning is shared across application modules.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public final class SettingKeys {

    public static final String SYSTEM_USER_TWO_FACTOR_REQUIRED =
            "security.system-users.two-factor-required";
    public static final String MERCHANT_USER_TWO_FACTOR_REQUIRED =
            "security.merchant-users.two-factor-required";

    private SettingKeys() {
    }
}
