package ir.jetvam.modules.origination.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.jetvam.common.time.ClockTimeProvider;
import ir.jetvam.modules.assessment.service.DefaultEligibilityPolicyEvaluator;
import ir.jetvam.modules.origination.model.ApplicationControlEntity;
import ir.jetvam.modules.origination.model.ApplicationControlStatus;
import ir.jetvam.modules.origination.model.ApplicationStatus;
import ir.jetvam.modules.origination.model.LoanApplicationEntity;
import ir.jetvam.modules.payment.service.PaymentService;
import ir.jetvam.modules.product.model.PlanControlType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Verifies strict-priority, fail-fast execution of local and inquiry-backed application controls.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
class OriginationControlOrchestratorTest {

    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");

    @Test
    void rejectsImmediatelyAndCancelsLaterInquiriesWhenALocalControlFails() {
        OriginationControlInquiryService inquiries = mock(OriginationControlInquiryService.class);
        OriginationControlOrchestrator orchestrator = orchestrator(inquiries);
        LoanApplicationEntity application = application(LocalDate.of(2015, 1, 1));
        ApplicationControlEntity age = age(application, 1);
        ApplicationControlEntity credit = credit(application, 2);
        application.addControl(age);
        application.addControl(credit);

        orchestrator.advance(application);

        assertThat(age.getStatus()).isEqualTo(ApplicationControlStatus.FAILED);
        assertThat(credit.getStatus()).isEqualTo(ApplicationControlStatus.CANCELLED);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        verify(inquiries, never()).submit(application, credit);
    }

    @Test
    void passesLocalControlsAndSubmitsOnlyTheFirstInquiryPriority() {
        OriginationControlInquiryService inquiries = mock(OriginationControlInquiryService.class);
        OriginationControlOrchestrator orchestrator = orchestrator(inquiries);
        LoanApplicationEntity application = application(LocalDate.of(1990, 1, 1));
        ApplicationControlEntity age = age(application, 1);
        ApplicationControlEntity credit = credit(application, 2);
        ApplicationControlEntity cheque = cheque(application, 3);
        application.addControl(age);
        application.addControl(credit);
        application.addControl(cheque);

        orchestrator.advance(application);

        assertThat(age.getStatus()).isEqualTo(ApplicationControlStatus.PASSED);
        assertThat(credit.getStatus()).isEqualTo(ApplicationControlStatus.PENDING_INQUIRY);
        assertThat(cheque.getStatus()).isEqualTo(ApplicationControlStatus.WAITING_PRIORITY);
        verify(inquiries).submit(application, credit);
        verify(inquiries, never()).submit(application, cheque);
    }

    @Test
    void callbackPassReleasesOnlyTheNextPriority() {
        OriginationControlInquiryService inquiries = mock(OriginationControlInquiryService.class);
        OriginationControlOrchestrator orchestrator = orchestrator(inquiries);
        LoanApplicationEntity application = application(LocalDate.of(1990, 1, 1));
        ApplicationControlEntity credit = credit(application, 1);
        ApplicationControlEntity cheque = cheque(application, 2);
        application.addControl(credit);
        application.addControl(cheque);
        credit.releaseInquiry();
        credit.inquirySubmitted(UUID.randomUUID());

        orchestrator.complete(application, credit, Map.of("rank", "8"));

        assertThat(credit.getStatus()).isEqualTo(ApplicationControlStatus.PASSED);
        assertThat(cheque.getStatus()).isEqualTo(ApplicationControlStatus.PENDING_INQUIRY);
        verify(inquiries).submit(application, cheque);
    }

    @Test
    void reusesFactsWhenTwoControlsDependOnTheSameInquiry() {
        OriginationControlInquiryService inquiries = mock(OriginationControlInquiryService.class);
        OriginationControlOrchestrator orchestrator = orchestrator(inquiries);
        LoanApplicationEntity application = application(LocalDate.of(1990, 1, 1));
        ApplicationControlEntity first = credit(application, 1);
        ApplicationControlEntity second = new ApplicationControlEntity(
                application, "CREDIT_STRICTER", "Credit stricter", 2,
                PlanControlType.MINIMUM_CREDIT_RANK, BigDecimal.valueOf(5), null,
                "CREDIT_RATING_INQUIRY", "Credit rank is too low"
        );
        application.addControl(first);
        application.addControl(second);
        first.releaseInquiry();
        first.inquirySubmitted(UUID.randomUUID());

        orchestrator.complete(application, first, Map.of("rank", "8"));

        assertThat(first.getStatus()).isEqualTo(ApplicationControlStatus.PASSED);
        assertThat(second.getStatus()).isEqualTo(ApplicationControlStatus.PASSED);
        assertThat(second.getInquiryRequestId()).isNull();
        verify(inquiries, never()).submit(application, second);
    }

    @Test
    void blocksTheCurrentInquiryUntilOnlyItsFeesArePaid() {
        OriginationControlInquiryService inquiries = mock(OriginationControlInquiryService.class);
        PaymentService payments = mock(PaymentService.class);
        LoanApplicationEntity application = application(LocalDate.of(1990, 1, 1));
        ApplicationControlEntity credit = credit(application, 1);
        application.addControl(credit);
        when(payments.allPaid(application.getId(), "CREDIT_RATING_INQUIRY")).thenReturn(false);
        OriginationControlOrchestrator orchestrator = orchestrator(inquiries, payments);

        orchestrator.advance(application);

        assertThat(credit.getStatus()).isEqualTo(ApplicationControlStatus.BLOCKED_BY_PAYMENT);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WAITING_CONTROL_FEE);
        verify(payments).activate(application.getId(), "CREDIT_RATING_INQUIRY");
        verify(inquiries, never()).submit(application, credit);
    }

    @Test
    void failedInquiryCancelsLaterControlsAndEveryUnpaidFee() {
        OriginationControlInquiryService inquiries = mock(OriginationControlInquiryService.class);
        PaymentService payments = mock(PaymentService.class);
        LoanApplicationEntity application = application(LocalDate.of(1990, 1, 1));
        ApplicationControlEntity credit = credit(application, 1);
        ApplicationControlEntity cheque = cheque(application, 2);
        application.addControl(credit);
        application.addControl(cheque);
        credit.releaseInquiry();
        credit.inquirySubmitted(UUID.randomUUID());
        OriginationControlOrchestrator orchestrator = orchestrator(inquiries, payments);

        orchestrator.complete(application, credit, Map.of("rank", "3"));

        assertThat(credit.getStatus()).isEqualTo(ApplicationControlStatus.FAILED);
        assertThat(cheque.getStatus()).isEqualTo(ApplicationControlStatus.CANCELLED);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
        verify(payments).cancelUnpaid(application.getId());
        verify(inquiries, never()).submit(eq(application), any(ApplicationControlEntity.class));
    }

    private static OriginationControlOrchestrator orchestrator(OriginationControlInquiryService inquiries) {
        PaymentService payments = mock(PaymentService.class);
        when(payments.allPaid(any(), anyString())).thenReturn(true);
        return orchestrator(inquiries, payments);
    }

    private static OriginationControlOrchestrator orchestrator(
            OriginationControlInquiryService inquiries,
            PaymentService payments
    ) {
        return new OriginationControlOrchestrator(
                new DefaultEligibilityPolicyEvaluator(),
                inquiries,
                payments,
                new ClockTimeProvider(Clock.fixed(NOW, ZoneOffset.UTC)),
                new ObjectMapper()
        );
    }

    private static LoanApplicationEntity application(LocalDate birthDate) {
        LoanApplicationEntity application = new LoanApplicationEntity(
                UUID.randomUUID(), "0013546789", birthDate, UUID.randomUUID(),
                "PLAN", "Plan", BigDecimal.valueOf(100_000_000), 12, BigDecimal.valueOf(23),
                false, false, false, null, null, ApplicationStatus.WAITING_CONTROLS
        );
        ReflectionTestUtils.setField(application, "id", UUID.randomUUID());
        return application;
    }

    private static ApplicationControlEntity age(LoanApplicationEntity application, int priority) {
        return new ApplicationControlEntity(
                application, "AGE", "Age", priority, PlanControlType.AGE_RANGE,
                BigDecimal.valueOf(18), BigDecimal.valueOf(65), null, "Age is not eligible"
        );
    }

    private static ApplicationControlEntity credit(LoanApplicationEntity application, int priority) {
        return new ApplicationControlEntity(
                application, "CREDIT", "Credit", priority, PlanControlType.MINIMUM_CREDIT_RANK,
                BigDecimal.valueOf(7), null, "CREDIT_RATING_INQUIRY", "Credit rank is too low"
        );
    }

    private static ApplicationControlEntity cheque(LoanApplicationEntity application, int priority) {
        return new ApplicationControlEntity(
                application, "CHEQUE", "Cheque", priority, PlanControlType.NO_BAD_CHEQUE,
                null, null, "BAD_CHEQUE_INQUIRY", "Applicant has an unsettled cheque"
        );
    }
}
