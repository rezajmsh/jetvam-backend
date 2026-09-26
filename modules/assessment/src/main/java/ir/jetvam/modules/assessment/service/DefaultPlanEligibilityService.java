package ir.jetvam.modules.assessment.service;

import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.inquiry.service.InquiryService;
import ir.jetvam.modules.product.service.ProductCatalogService;
import ir.jetvam.modules.product.service.ProductViews;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Evaluates enabled plan controls and executes only the external inquiries they require.
 * Product lookup completes before provider calls, so no database transaction spans network I/O.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultPlanEligibilityService implements PlanEligibilityService {

    private final ProductCatalogService productCatalogService;
    private final InquiryService inquiryService;
    private final TimeProvider timeProvider;

    @Override
    public AssessmentModels.Result assess(AssessmentModels.Command command) {
        Preconditions.requireNonNull(command, "command");
        LocalDate birthDate = Preconditions.requireNonNull(command.birthDate(), "birthDate");
        LocalDate today = timeProvider.today();
        Preconditions.require(birthDate.isBefore(today), "birthDate must be in the past");
        String nationalCode = IranianIdentifiers.normalizeNationalCode(command.nationalCode());
        Preconditions.require(IranianIdentifiers.isValidNationalCode(nationalCode), "nationalCode is invalid");

        ProductViews.Plan plan = productCatalogService.getActivePlan(command.planId());
        List<AssessmentModels.ControlResult> outcomes = new ArrayList<>();
        InquiryResults.CreditRating creditRating = null;
        InquiryResults.BadCheque badCheque = null;

        for (ProductViews.Control control : plan.controls().stream()
                .sorted(Comparator.comparingInt(ProductViews.Control::priority)).toList()) {
            switch (control.type()) {
                case AGE_RANGE -> outcomes.add(evaluateAge(control, birthDate, today));
                case MINIMUM_CREDIT_RANK -> {
                    if (creditRating == null) {
                        creditRating = inquiryService.findCreditRating(new InquiryRequests.CreditRating(nationalCode));
                    }
                    outcomes.add(evaluateCreditRank(control, creditRating));
                }
                case NO_BAD_CHEQUE -> {
                    if (badCheque == null) {
                        badCheque = inquiryService.findBadCheques(new InquiryRequests.BadCheque(nationalCode));
                    }
                    outcomes.add(evaluateBadCheque(control, badCheque));
                }
            }
            if (!outcomes.getLast().passed()) {
                break;
            }
        }
        return new AssessmentModels.Result(
                plan.id(),
                outcomes.stream().allMatch(AssessmentModels.ControlResult::passed),
                List.copyOf(outcomes)
        );
    }

    private static AssessmentModels.ControlResult evaluateAge(
            ProductViews.Control control,
            LocalDate birthDate,
            LocalDate today
    ) {
        int age = Period.between(birthDate, today).getYears();
        boolean minimumPassed = control.minimumValue() == null
                || BigDecimal.valueOf(age).compareTo(control.minimumValue()) >= 0;
        boolean maximumPassed = control.maximumValue() == null
                || BigDecimal.valueOf(age).compareTo(control.maximumValue()) <= 0;
        return outcome(control, minimumPassed && maximumPassed, Integer.toString(age), null);
    }

    private static AssessmentModels.ControlResult evaluateCreditRank(
            ProductViews.Control control,
            InquiryResults.CreditRating rating
    ) {
        boolean passed = BigDecimal.valueOf(rating.rank()).compareTo(control.minimumValue()) >= 0;
        String observed = rating.ratingCode() + " (rank=" + rating.rank() + ")";
        return outcome(control, passed, observed, rating.trackingId());
    }

    private static AssessmentModels.ControlResult evaluateBadCheque(
            ProductViews.Control control,
            InquiryResults.BadCheque badCheque
    ) {
        return outcome(
                control,
                !badCheque.hasUnsettledCheques(),
                Integer.toString(badCheque.unsettledCount()),
                badCheque.trackingId()
        );
    }

    private static AssessmentModels.ControlResult outcome(
            ProductViews.Control control,
            boolean passed,
            String observed,
            String trackingId
    ) {
        return new AssessmentModels.ControlResult(
                control.code(), control.type(), passed, observed, trackingId,
                passed ? null : control.failureMessage()
        );
    }
}
