package ir.jetvam.modules.assessment;

/**
 * Declares the authority required to execute plan eligibility assessments directly.
 * Customer workflows call this capability through Origination after ownership validation.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class AssessmentPermissions {

    public static final String EXECUTE = "assessment:execute";

    private AssessmentPermissions() {
    }
}
