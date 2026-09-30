package ir.jetvam.modules.origination.model;

import ir.jetvam.common.inquiry.InquiryType;
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
        LoanApplicationEntity application = application(ApplicationStatus.WAITING_EMPLOYMENT_INFORMATION);
        Instant completedAt = Instant.parse("2026-09-25T10:00:00Z");

        application.employmentInformationSaved(true);
        application.signContract(completedAt.minusSeconds(60));
        application.allocateCredit(completedAt);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.COMPLETED);
        assertThat(application.getCompletedAt()).isEqualTo(completedAt);
    }

    private static LoanApplicationEntity application(ApplicationStatus status) {
        return new LoanApplicationEntity(
                UUID.randomUUID(), "0013546789", LocalDate.of(1990, 1, 1), UUID.randomUUID(),
                "PLAN", "Plan", BigDecimal.valueOf(100_000_000), 12, BigDecimal.valueOf(23),
                false, status
        );
    }

    private static ApplicationControlEntity control(
            LoanApplicationEntity application,
            String code,
            int priority
    ) {
        return new ApplicationControlEntity(
                application, ir.jetvam.modules.product.model.ControlSubjectType.APPLICANT,
                application.getCustomerPartyId(), code, code, priority,
                ir.jetvam.modules.product.model.PlanControlType.AGE_RANGE,
                java.math.BigDecimal.valueOf(18), null, null, "Control failed"
        );
    }

    @Test
    void requiresApplicationEmploymentSnapshotAfterControls() {
        LoanApplicationEntity application = new LoanApplicationEntity(
                UUID.randomUUID(), "0013546789", LocalDate.of(1990, 1, 1), UUID.randomUUID(),
                "PLAN", "Plan", BigDecimal.valueOf(100_000_000), 12, BigDecimal.valueOf(23),
                false, ApplicationStatus.WAITING_CONTROLS
        );

        application.controlsPassed();

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WAITING_EMPLOYMENT_INFORMATION);
    }

    @Test
    void guarantorControlWaitsUntilItsPartyIsKnown() {
        LoanApplicationEntity application = application(ApplicationStatus.WAITING_CONTROLS);
        ApplicationControlEntity control = new ApplicationControlEntity(
                application, ir.jetvam.modules.product.model.ControlSubjectType.GUARANTOR, null,
                "GUARANTOR_CHEQUE", "Guarantor cheque", 1,
                ir.jetvam.modules.product.model.PlanControlType.NO_BAD_CHEQUE,
                null, null, InquiryType.BAD_CHEQUE, "Control failed"
        );

        assertThat(control.getStatus()).isEqualTo(ApplicationControlStatus.WAITING_SUBJECT);
        UUID guarantorPartyId = UUID.randomUUID();
        control.assignSubject(guarantorPartyId);
        assertThat(control.getSubjectPartyId()).isEqualTo(guarantorPartyId);
        assertThat(control.getStatus()).isEqualTo(ApplicationControlStatus.WAITING_PRIORITY);
    }

    @Test
    void derivesPhysicalDeliveryStageFromSnapshottedCollateral() {
        LoanApplicationEntity application = application(ApplicationStatus.WAITING_EMPLOYMENT_INFORMATION);
        ApplicationCollateralEntity collateral = new ApplicationCollateralEntity(
                application, CollateralProviderType.APPLICANT, application.getCustomerPartyId(), null,
                UUID.randomUUID(), "SAYAD_CHEQUE", "Sayad cheque", "SAYAD_CHEQUE",
                BigDecimal.valueOf(100), true, true
        );
        application.addCollateral(collateral);
        Instant at = Instant.parse("2026-09-29T08:00:00Z");

        application.employmentInformationSaved(true);
        collateral.informationCompleted(at);
        application.guaranteeInformationCompleted(true);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WAITING_ORIGINAL_COLLATERAL);
        collateral.originalReceived(at.plusSeconds(60));
        application.originalCollateralsReceived();
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WAITING_SIGNATURE);
    }

    @Test
    void keepsCollateralIncompleteUntilRequiredDocumentIsUploaded() {
        LoanApplicationEntity application = application(ApplicationStatus.WAITING_GUARANTEE);
        ApplicationCollateralEntity collateral = new ApplicationCollateralEntity(
                application, CollateralProviderType.APPLICANT, application.getCustomerPartyId(), null,
                UUID.randomUUID(), "SAYAD_CHEQUE", "Sayad cheque", "SAYAD_CHEQUE",
                BigDecimal.valueOf(100), true, true
        );
        ApplicationCollateralDocumentRequirementEntity requirement =
                new ApplicationCollateralDocumentRequirementEntity(
                        collateral, UUID.randomUUID(), "CHEQUE_FRONT_IMAGE", "Cheque front image",
                        "image/jpeg,image/png", 5_000_000, true, 1, 1, 10
                );
        collateral.addDocumentRequirement(requirement);
        Instant at = Instant.parse("2026-09-29T08:00:00Z");

        collateral.informationCompleted(at);

        assertThat(collateral.getStatus()).isEqualTo(ApplicationCollateralStatus.WAITING_INFORMATION);
        requirement.addDocument(new ApplicationDocumentEntity(
                application, requirement, application.getId() + "/front.jpg", "front.jpg", "image/jpeg",
                10, "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                application.getCustomerPartyId()
        ));
        collateral.documentsUpdated();

        assertThat(collateral.getStatus()).isEqualTo(ApplicationCollateralStatus.WAITING_ORIGINAL_DELIVERY);
        assertThat(requirement.isSatisfied()).isTrue();
    }
}
