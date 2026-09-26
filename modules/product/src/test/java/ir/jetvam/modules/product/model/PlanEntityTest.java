package ir.jetvam.modules.product.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Verifies plan aggregate invariants without involving persistence or provider implementations.
 * The tests cover optional requirements and cross-reference validation for inquiry fees.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
class PlanEntityTest {

    @Test
    void acceptsAPlanWithoutChequeOrGuarantorRequirements() {
        PlanEntity plan = newPlan();

        plan.replaceConfiguration(
                List.of(new PlanInquiryEntity(
                        plan, "BAD_CHEQUE", "Bad cheque inquiry", "BANKING", 1, true, true, "{}"
                )),
                List.of(),
                List.of(),
                List.of(new PlanFeeEntity(
                        plan, "INQUIRY_FEE", "Inquiry fee", BigDecimal.valueOf(100_000), "IRR",
                        "BEFORE_INQUIRIES", "BAD_CHEQUE", false, true
                )),
                List.of(new PlanControlEntity(
                        plan, "NO_BAD_CHEQUE", "No bad cheque", 1, PlanControlType.NO_BAD_CHEQUE,
                        null, null, "BAD_CHEQUE", "Applicant has an unsettled cheque", true
                ))
        );

        assertThat(plan.getGuarantees()).isEmpty();
        assertThat(plan.getCollaterals()).isEmpty();
        assertThat(plan.getInquiries()).extracting(PlanInquiryEntity::getCode).containsExactly("BAD_CHEQUE");
    }

    @Test
    void rejectsAFeeThatReferencesAnInquiryOutsideThePlan() {
        PlanEntity plan = newPlan();
        PlanFeeEntity fee = new PlanFeeEntity(
                plan, "INQUIRY_FEE", "Inquiry fee", BigDecimal.TEN, "IRR",
                "BEFORE_INQUIRIES", "CREDIT_SCORE", false, true
        );

        assertThatIllegalArgumentException().isThrownBy(() -> plan.replaceConfiguration(
                List.of(), List.of(), List.of(), List.of(fee), List.of()
        )).withMessageContaining("sourceInquiryCode");
    }

    @Test
    void rejectsDuplicateInquiryOrder() {
        PlanEntity plan = newPlan();

        assertThatIllegalArgumentException().isThrownBy(() -> plan.replaceConfiguration(
                List.of(
                        new PlanInquiryEntity(plan, "BAD_CHEQUE", "Bad cheque", "BANKING", 1, true, true, "{}"),
                        new PlanInquiryEntity(plan, "CREDIT_SCORE", "Credit score", "SCORING", 1, true, true, "{}")
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        )).withMessageContaining("Duplicate inquiry sequence");
    }

    @Test
    void rejectsAnInquiryBasedControlWithoutItsConfiguredInquiry() {
        PlanEntity plan = newPlan();
        PlanControlEntity control = new PlanControlEntity(
                plan, "MINIMUM_RATING", "Minimum credit rating", 1, PlanControlType.MINIMUM_CREDIT_RANK,
                BigDecimal.valueOf(7), null, "CREDIT_RATING", "Credit rating is too low", true
        );

        assertThatIllegalArgumentException().isThrownBy(() -> plan.replaceConfiguration(
                List.of(), List.of(), List.of(), List.of(), List.of(control)
        )).withMessageContaining("Control sourceInquiryCode");
    }

    @Test
    void rejectsAnEnabledInquiryThatIsNotBehindAControl() {
        PlanEntity plan = newPlan();

        assertThatIllegalArgumentException().isThrownBy(() -> plan.replaceConfiguration(
                List.of(new PlanInquiryEntity(
                        plan, "UNUSED", "Unused inquiry", "BANKING", 1, true, true, "{}"
                )),
                List.of(), List.of(), List.of(), List.of()
        )).withMessageContaining("referenced by an enabled control");
    }

    @Test
    void rejectsDuplicateControlPriorityToKeepFailFastExecutionDeterministic() {
        PlanEntity plan = newPlan();
        PlanControlEntity age = new PlanControlEntity(
                plan, "AGE", "Age", 1, PlanControlType.AGE_RANGE,
                BigDecimal.valueOf(18), null, null, "Applicant is too young", true
        );
        PlanControlEntity cheque = new PlanControlEntity(
                plan, "CHEQUE", "Cheque", 1, PlanControlType.NO_BAD_CHEQUE,
                null, null, "BAD_CHEQUE", "Applicant has an unsettled cheque", true
        );

        assertThatIllegalArgumentException().isThrownBy(() -> plan.replaceConfiguration(
                List.of(new PlanInquiryEntity(
                        plan, "BAD_CHEQUE", "Bad cheque", "BANKING", 1, true, true, "{}"
                )),
                List.of(), List.of(), List.of(), List.of(age, cheque)
        )).withMessageContaining("Duplicate control priority");
    }

    private static PlanEntity newPlan() {
        ProductEntity product = new ProductEntity("LOAN", "Loan", null);
        return new PlanEntity(
                product, "LEVEL_10", "Level 10", null, BigDecimal.ZERO, BigDecimal.valueOf(100_000_000),
                12, 24, BigDecimal.valueOf(23)
        );
    }
}
