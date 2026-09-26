package ir.jetvam.modules.assessment.service;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.product.model.PlanControlType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

/**
 * Applies age, credit-rank and bad-cheque policies without performing external calls.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
public class DefaultEligibilityPolicyEvaluator implements EligibilityPolicyEvaluator {

    @Override
    public AssessmentModels.PolicyResult evaluate(
            List<AssessmentModels.PolicyControl> controls,
            AssessmentModels.Facts facts,
            LocalDate assessmentDate
    ) {
        Preconditions.requireNonNull(controls, "controls");
        Preconditions.requireNonNull(facts, "facts");
        Preconditions.requireNonNull(assessmentDate, "assessmentDate");
        List<AssessmentModels.ControlResult> results = controls.stream()
                .map(control -> evaluate(control, facts, assessmentDate))
                .toList();
        return new AssessmentModels.PolicyResult(
                results.stream().allMatch(AssessmentModels.ControlResult::passed),
                results
        );
    }

    private static AssessmentModels.ControlResult evaluate(
            AssessmentModels.PolicyControl control,
            AssessmentModels.Facts facts,
            LocalDate assessmentDate
    ) {
        return switch (control.type()) {
            case AGE_RANGE -> age(control, facts.birthDate(), assessmentDate);
            case MINIMUM_CREDIT_RANK -> creditRank(control, facts.creditRank());
            case NO_BAD_CHEQUE -> badCheque(control, facts.hasBadCheque());
        };
    }

    private static AssessmentModels.ControlResult age(
            AssessmentModels.PolicyControl control,
            LocalDate birthDate,
            LocalDate assessmentDate
    ) {
        int age = Period.between(Preconditions.requireNonNull(birthDate, "birthDate"), assessmentDate).getYears();
        boolean passed = (control.minimumValue() == null
                || BigDecimal.valueOf(age).compareTo(control.minimumValue()) >= 0)
                && (control.maximumValue() == null
                || BigDecimal.valueOf(age).compareTo(control.maximumValue()) <= 0);
        return outcome(control, passed, Integer.toString(age));
    }

    private static AssessmentModels.ControlResult creditRank(
            AssessmentModels.PolicyControl control,
            Integer creditRank
    ) {
        int rank = Preconditions.requireNonNull(creditRank, "creditRank");
        boolean passed = BigDecimal.valueOf(rank).compareTo(control.minimumValue()) >= 0;
        return outcome(control, passed, Integer.toString(rank));
    }

    private static AssessmentModels.ControlResult badCheque(
            AssessmentModels.PolicyControl control,
            Boolean hasBadCheque
    ) {
        boolean observed = Preconditions.requireNonNull(hasBadCheque, "hasBadCheque");
        return outcome(control, !observed, Boolean.toString(observed));
    }

    private static AssessmentModels.ControlResult outcome(
            AssessmentModels.PolicyControl control,
            boolean passed,
            String observed
    ) {
        return new AssessmentModels.ControlResult(
                control.code(), control.type(), passed, observed, null,
                passed ? null : control.failureMessage()
        );
    }
}
