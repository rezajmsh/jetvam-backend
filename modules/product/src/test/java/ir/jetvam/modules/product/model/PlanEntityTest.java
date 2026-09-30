package ir.jetvam.modules.product.model;

import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.infra.persistence.entity.AbstractUuidEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Verifies plan aggregate invariants for centralized, subject-aware controls.
 * Inquiry dependencies are derived exclusively from selected control definitions.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
class PlanEntityTest {

    @Test
    void acceptsAnInquiryBackedControlAndItsFee() {
        PlanEntity plan = newPlan();
        ControlDefinitionEntity definition = definition(
                "NO_BAD_CHEQUE", PlanControlType.NO_BAD_CHEQUE, InquiryType.BAD_CHEQUE
        );

        plan.replaceConfiguration(
                List.of(), List.of(), List.of(),
                List.of(new PlanFeeEntity(plan, fee("INQUIRY_FEE", InquiryType.BAD_CHEQUE), true)),
                List.of(control(plan, definition, ControlSubjectType.APPLICANT, 1))
        );

        assertThat(plan.getControls()).hasSize(1);
        assertThat(plan.getFees()).hasSize(1);
    }

    @Test
    void rejectsAFeeWithoutAnEnabledControlForItsInquiry() {
        PlanEntity plan = newPlan();

        assertThatIllegalArgumentException().isThrownBy(() -> plan.replaceConfiguration(
                List.of(), List.of(), List.of(),
                List.of(new PlanFeeEntity(plan, fee("INQUIRY_FEE", InquiryType.CREDIT_RATING), true)),
                List.of()
        )).withMessageContaining("sourceInquiryCode");
    }

    @Test
    void rejectsDuplicateControlPriorityForTheSameSubject() {
        PlanEntity plan = newPlan();
        PlanControlEntity age = control(plan,
                definition("AGE", PlanControlType.AGE_RANGE, null), ControlSubjectType.APPLICANT, 1);
        PlanControlEntity cheque = control(plan,
                definition("CHEQUE", PlanControlType.NO_BAD_CHEQUE, InquiryType.BAD_CHEQUE),
                ControlSubjectType.APPLICANT, 1);

        assertThatIllegalArgumentException().isThrownBy(() -> plan.replaceConfiguration(
                List.of(), List.of(), List.of(), List.of(), List.of(age, cheque)
        )).withMessageContaining("Duplicate control priority for subject");
    }

    @Test
    void allowsTheSamePriorityForApplicantAndGuarantor() {
        PlanEntity plan = newPlan();
        ControlDefinitionEntity definition = definition(
                "NO_BAD_CHEQUE", PlanControlType.NO_BAD_CHEQUE, InquiryType.BAD_CHEQUE
        );

        plan.replaceConfiguration(
                List.of(), List.of(), List.of(), List.of(),
                List.of(
                        control(plan, definition, ControlSubjectType.APPLICANT, 1),
                        control(plan, definition, ControlSubjectType.GUARANTOR, 1)
                )
        );

        assertThat(plan.getControls()).hasSize(2);
    }

    @Test
    void updatesExistingCollateralInsteadOfReinsertingTheSameNaturalKey() {
        PlanEntity plan = newPlan();
        CollateralTypeEntity collateralType = new CollateralTypeEntity(
                "SAYAD_CHEQUE", "Sayad cheque", "SAYAD_CHEQUE", true, null, true
        );
        set(AbstractUuidEntity.class, collateralType, "id", UUID.randomUUID());
        plan.replaceConfiguration(
                List.of(), List.of(),
                List.of(new PlanCollateralEntity(plan, collateralType, BigDecimal.valueOf(100), true, true)),
                List.of(), List.of()
        );
        PlanCollateralEntity original = plan.getCollaterals().iterator().next();

        plan.replaceConfiguration(
                List.of(), List.of(),
                List.of(new PlanCollateralEntity(plan, collateralType, BigDecimal.valueOf(125), false, true)),
                List.of(), List.of()
        );

        assertThat(plan.getCollaterals()).containsExactly(original);
        assertThat(original.getMinimumCoveragePercent()).isEqualByComparingTo("125");
        assertThat(original.isRequired()).isFalse();
    }

    private static PlanControlEntity control(
            PlanEntity plan,
            ControlDefinitionEntity definition,
            ControlSubjectType subject,
            int priority
    ) {
        return new PlanControlEntity(plan, definition, subject, priority, true, List.of());
    }

    private static ControlDefinitionEntity definition(
            String code,
            PlanControlType type,
            InquiryType inquiryCode
    ) {
        ControlDefinitionEntity entity = new ControlDefinitionEntity();
        set(AbstractUuidEntity.class, entity, "id", UUID.randomUUID());
        set(ControlDefinitionEntity.class, entity, "code", code);
        set(ControlDefinitionEntity.class, entity, "title", code);
        set(ControlDefinitionEntity.class, entity, "evaluatorType", type);
        set(ControlDefinitionEntity.class, entity, "inquiryCode", inquiryCode);
        set(ControlDefinitionEntity.class, entity, "defaultFailureMessage", "Control failed");
        set(ControlDefinitionEntity.class, entity, "active", true);
        return entity;
    }

    private static void set(Class<?> owner, Object target, String name, Object value) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static PlanEntity newPlan() {
        ProductEntity product = new ProductEntity("LOAN", "Loan", null);
        return new PlanEntity(
                product, "LEVEL_10", "Level 10", null, BigDecimal.ZERO, BigDecimal.valueOf(100_000_000),
                12, 24, BigDecimal.valueOf(23)
        );
    }

    private static FeeDefinitionEntity fee(String code, InquiryType inquiryCode) {
        return new FeeDefinitionEntity(
                code, "Inquiry fee", ProductFeeCategory.INQUIRY, BigDecimal.TEN, "IRR",
                inquiryCode.code(), inquiryCode, false, true
        );
    }
}
