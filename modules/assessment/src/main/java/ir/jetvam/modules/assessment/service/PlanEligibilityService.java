package ir.jetvam.modules.assessment.service;

/**
 * Defines the application boundary for evaluating an applicant against one active plan.
 * Authoritative applicant facts are supplied by the owning workflow, not trusted customer input.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface PlanEligibilityService {

    AssessmentModels.Result assess(AssessmentModels.Command command);
}
