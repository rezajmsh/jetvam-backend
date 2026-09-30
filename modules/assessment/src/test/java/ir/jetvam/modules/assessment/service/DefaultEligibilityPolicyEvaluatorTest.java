package ir.jetvam.modules.assessment.service;

import ir.jetvam.modules.product.model.PlanControlType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultEligibilityPolicyEvaluatorTest {

    private final DefaultEligibilityPolicyEvaluator evaluator = new DefaultEligibilityPolicyEvaluator();

    @Test
    void evaluatesCollectedFactsWithoutCallingProviders() {
        List<AssessmentModels.PolicyControl> controls = List.of(
                new AssessmentModels.PolicyControl(
                        "AGE", PlanControlType.AGE_RANGE, BigDecimal.valueOf(18), BigDecimal.valueOf(70), "age"
                ),
                new AssessmentModels.PolicyControl(
                        "RANK", PlanControlType.MINIMUM_CREDIT_RANK, BigDecimal.valueOf(3), null, "rank"
                ),
                new AssessmentModels.PolicyControl(
                        "CHEQUE", PlanControlType.NO_BAD_CHEQUE, null, null, "cheque"
                )
        );

        AssessmentModels.PolicyResult result = evaluator.evaluate(
                controls,
                new AssessmentModels.Facts(LocalDate.of(1990, 1, 1), 4, 0, BigDecimal.ZERO),
                LocalDate.of(2026, 9, 25)
        );

        assertThat(result.eligible()).isTrue();
        assertThat(result.controls()).allMatch(AssessmentModels.ControlResult::passed);
    }

    @Test
    void evaluatesMaximumBadChequeCountAndAmountIndependently() {
        List<AssessmentModels.PolicyControl> controls = List.of(
                new AssessmentModels.PolicyControl(
                        "CHEQUE_COUNT", PlanControlType.MAXIMUM_BAD_CHEQUE_COUNT,
                        null, BigDecimal.valueOf(2), "count"
                ),
                new AssessmentModels.PolicyControl(
                        "CHEQUE_AMOUNT", PlanControlType.MAXIMUM_BAD_CHEQUE_AMOUNT,
                        null, BigDecimal.valueOf(50_000_000), "amount"
                )
        );

        AssessmentModels.PolicyResult result = evaluator.evaluate(
                controls,
                new AssessmentModels.Facts(
                        LocalDate.of(1990, 1, 1), null, 2, BigDecimal.valueOf(70_000_000)
                ),
                LocalDate.of(2026, 9, 25)
        );

        assertThat(result.controls()).extracting(AssessmentModels.ControlResult::passed)
                .containsExactly(true, false);
    }
}
