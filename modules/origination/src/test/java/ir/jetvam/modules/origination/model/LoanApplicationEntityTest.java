package ir.jetvam.modules.origination.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the loan-application state machine and stage-based inquiry release rules.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
class LoanApplicationEntityTest {

    @Test
    void movesToControlProcessingAfterInquiryFeePayment() {
        LoanApplicationEntity application = application(ApplicationStatus.WAITING_CONTROL_FEE);

        application.controlsPaid();

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WAITING_CONTROLS);
    }

    @Test
    void cancelsOnlyNonTerminalControlsAfterFailFastRejection() {
        LoanApplicationEntity application = application(ApplicationStatus.WAITING_CONTROLS);
        ApplicationControlEntity passed = control(application, "AGE", 1);
        ApplicationControlEntity waiting = control(application, "CREDIT", 2);
        passed.pass("35", null, Instant.parse("2026-09-25T10:00:00Z"));
        application.addControl(passed);
        application.addControl(waiting);

        application.cancelRemainingControls();

        assertThat(passed.getStatus()).isEqualTo(ApplicationControlStatus.PASSED);
        assertThat(waiting.getStatus()).isEqualTo(ApplicationControlStatus.CANCELLED);
    }

    @Test
    void progressesFromCustomerInformationToAllocation() {
        LoanApplicationEntity application = application(ApplicationStatus.WAITING_PERSONAL_INFORMATION);
        Instant completedAt = Instant.parse("2026-09-25T10:00:00Z");

        application.usePersonalProfile(1, null, true);
        application.useEmploymentProfile(1, true);
        application.signContract(completedAt.minusSeconds(60));
        application.allocateCredit(completedAt);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        assertThat(application.getCompletedAt()).isEqualTo(completedAt);
    }

    private static LoanApplicationEntity application(ApplicationStatus status) {
        return new LoanApplicationEntity(
                UUID.randomUUID(), "0013546789", LocalDate.of(1990, 1, 1), UUID.randomUUID(),
                "PLAN", "Plan", BigDecimal.valueOf(100_000_000), 12, BigDecimal.valueOf(23),
                false, false, false, null, null, status
        );
    }

    private static ApplicationControlEntity control(
            LoanApplicationEntity application,
            String code,
            int priority
    ) {
        return new ApplicationControlEntity(
                application, code, code, priority,
                ir.jetvam.modules.product.model.PlanControlType.AGE_RANGE,
                java.math.BigDecimal.valueOf(18), null, null, "Control failed"
        );
    }

    @Test
    void skipsAlreadyCompletedProfileStagesAfterInquiries() {
        LoanApplicationEntity application = new LoanApplicationEntity(
                UUID.randomUUID(), "0013546789", LocalDate.of(1990, 1, 1), UUID.randomUUID(),
                "PLAN", "Plan", BigDecimal.valueOf(100_000_000), 12, BigDecimal.valueOf(23),
                false, false, false, 3L, 2L, ApplicationStatus.WAITING_CONTROLS
        );

        application.controlsPassed();

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WAITING_SIGNATURE);
        assertThat(application.getPersonalProfileRevision()).isEqualTo(3L);
        assertThat(application.getEmploymentProfileRevision()).isEqualTo(2L);
    }
}
