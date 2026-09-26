package ir.jetvam.modules.payment;

/**
 * Declares granular authorities exposed by the payment module.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class PaymentPermissions {

    public static final String SELF_READ = "payment:self:read";
    public static final String SELF_WRITE = "payment:self:write";
    public static final String CONFIRM = "payment:confirm";

    private PaymentPermissions() {
    }
}
