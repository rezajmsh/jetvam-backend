package ir.jetvam.modules.assessment.service;

import ir.jetvam.common.time.ClockTimeProvider;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.inquiry.service.InquiryService;
import ir.jetvam.modules.product.model.PlanControlType;
import ir.jetvam.modules.product.model.ControlSubjectType;
import ir.jetvam.modules.product.model.PublicationStatus;
import ir.jetvam.modules.product.service.ProductCatalogService;
import ir.jetvam.modules.product.service.ProductViews;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that plan controls request only their required inquiries and evaluate normalized facts.
 * External calls occur after the Product service has returned an immutable plan view.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
class DefaultPlanEligibilityServiceTest {

    private static final String NATIONAL_CODE = "0067749828";

    @Test
    void evaluatesAgeCreditRankAndBadChequeControls() {
        UUID planId = UUID.randomUUID();
        ProductCatalogService products = mock(ProductCatalogService.class);
        InquiryService inquiries = mock(InquiryService.class);
        when(products.getActivePlan(planId)).thenReturn(plan(
                planId,
                control(
                        "AGE", "Age", 1, PlanControlType.AGE_RANGE, BigDecimal.valueOf(18),
                        BigDecimal.valueOf(65), null, "Age is not eligible", true
                ),
                control(
                        "CREDIT", "Credit", 2, PlanControlType.MINIMUM_CREDIT_RANK, BigDecimal.valueOf(7),
                        null, "CREDIT_RATING", "Credit rating is too low", true
                ),
                control(
                        "CHEQUE", "Cheque", 3, PlanControlType.NO_BAD_CHEQUE, null,
                        null, "BAD_CHEQUE", "Applicant has unsettled cheques", true
                )
        ));
        when(inquiries.findCreditRating(new InquiryRequests.CreditRating(NATIONAL_CODE)))
                .thenReturn(new InquiryResults.CreditRating("B2", 6, BigDecimal.valueOf(610), "rating-1"));
        when(inquiries.findBadCheques(new InquiryRequests.BadCheque(NATIONAL_CODE)))
                .thenReturn(new InquiryResults.BadCheque(0, BigDecimal.ZERO, "cheque-1"));
        DefaultPlanEligibilityService service = new DefaultPlanEligibilityService(
                products,
                inquiries,
                new ClockTimeProvider(Clock.fixed(Instant.parse("2026-09-25T00:00:00Z"), ZoneOffset.UTC))
        );

        AssessmentModels.Result result = service.assess(new AssessmentModels.Command(
                planId, NATIONAL_CODE, LocalDate.of(1990, 9, 25)
        ));

        assertThat(result.eligible()).isFalse();
        assertThat(result.controls()).extracting(AssessmentModels.ControlResult::passed)
                .containsExactly(true, false);
        verify(products).getActivePlan(planId);
        verify(inquiries).findCreditRating(new InquiryRequests.CreditRating(NATIONAL_CODE));
        verify(inquiries, never()).findBadCheques(new InquiryRequests.BadCheque(NATIONAL_CODE));
    }

    @Test
    void ageOnlyPlanDoesNotExecuteExternalInquiries() {
        UUID planId = UUID.randomUUID();
        ProductCatalogService products = mock(ProductCatalogService.class);
        InquiryService inquiries = mock(InquiryService.class);
        when(products.getActivePlan(planId)).thenReturn(plan(
                planId,
                control(
                        "AGE", "Age", 1, PlanControlType.AGE_RANGE, BigDecimal.valueOf(18),
                        null, null, "Applicant is too young", true
                )
        ));
        DefaultPlanEligibilityService service = new DefaultPlanEligibilityService(
                products,
                inquiries,
                new ClockTimeProvider(Clock.fixed(Instant.parse("2026-09-25T00:00:00Z"), ZoneOffset.UTC))
        );

        AssessmentModels.Result result = service.assess(new AssessmentModels.Command(
                planId, NATIONAL_CODE, LocalDate.of(2000, 1, 1)
        ));

        assertThat(result.eligible()).isTrue();
        verify(inquiries, never()).findCreditRating(org.mockito.ArgumentMatchers.any());
        verify(inquiries, never()).findBadCheques(org.mockito.ArgumentMatchers.any());
    }

    private static ProductViews.Plan plan(UUID planId, ProductViews.Control... controls) {
        return new ProductViews.Plan(
                planId,
                UUID.randomUUID(),
                "PLAN",
                "Plan",
                null,
                BigDecimal.ZERO,
                BigDecimal.valueOf(100_000_000),
                12,
                24,
                BigDecimal.valueOf(23),
                PublicationStatus.ACTIVE,
                0,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(controls)
        );
    }

    private static ProductViews.Control control(
            String code,
            String title,
            int priority,
            PlanControlType type,
            BigDecimal minimumValue,
            BigDecimal maximumValue,
            String sourceInquiryCode,
            String failureMessage,
            boolean enabled
    ) {
        return new ProductViews.Control(
                UUID.randomUUID(), code, title, priority, type, ControlSubjectType.APPLICANT,
                minimumValue, maximumValue, sourceInquiryCode, failureMessage, List.of(), enabled
        );
    }
}
