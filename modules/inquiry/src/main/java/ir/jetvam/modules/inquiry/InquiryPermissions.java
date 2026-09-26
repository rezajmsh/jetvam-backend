package ir.jetvam.modules.inquiry;

/**
 * Declares the authority required for direct operational inquiry execution.
 * Customer-facing workflows invoke inquiry services internally after their own ownership checks.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class InquiryPermissions {

    public static final String EXECUTE = "inquiry:execute";

    private InquiryPermissions() {
    }
}
