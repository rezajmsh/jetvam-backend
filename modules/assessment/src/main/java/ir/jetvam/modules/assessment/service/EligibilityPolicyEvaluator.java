package ir.jetvam.modules.assessment.service;

import java.time.LocalDate;
import java.util.List;

/**
 * Evaluates snapshotted plan controls from already collected applicant and inquiry facts.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface EligibilityPolicyEvaluator {

    AssessmentModels.PolicyResult evaluate(
            List<AssessmentModels.PolicyControl> controls,
            AssessmentModels.Facts facts,
            LocalDate assessmentDate
    );
}
