package ir.jetvam.modules.assessment.service;

import ir.jetvam.modules.product.model.PlanControlType;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Groups immutable commands and outcomes for plan eligibility assessment.
 * Each control outcome includes the normalized observed value and external tracking reference when applicable.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class AssessmentModels {

    private AssessmentModels() {
    }

    public record Command(UUID planId, String nationalCode, LocalDate birthDate) {
    }

    public record Result(UUID planId, boolean eligible, List<ControlResult> controls) {
    }

    public record ControlResult(
            String code,
            PlanControlType type,
            boolean passed,
            String observedValue,
            String trackingId,
            String failureMessage
    ) {
    }

    public record PolicyControl(
            String code,
            PlanControlType type,
            BigDecimal minimumValue,
            BigDecimal maximumValue,
            String failureMessage
    ) {
    }

    public record Facts(LocalDate birthDate, Integer creditRank, Boolean hasBadCheque) {
    }

    public record PolicyResult(boolean eligible, List<ControlResult> controls) {
    }
}
