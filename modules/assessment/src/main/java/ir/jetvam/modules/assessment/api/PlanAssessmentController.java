package ir.jetvam.modules.assessment.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.modules.assessment.service.AssessmentModels;
import ir.jetvam.modules.assessment.service.PlanEligibilityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Exposes a protected assessment endpoint for trusted workflows and operational users.
 * The customer frontend must use Origination, which supplies authoritative identity facts.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@RestController
@RequestMapping("/api/v1/assessments/plans")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jetvam.assessment.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR', 'SERVICE') and hasAuthority('assessment:execute')")
@Tag(name = "Plan assessment", description = "Evaluates applicant facts and inquiry outcomes against plan controls.")
public class PlanAssessmentController {

    private final PlanEligibilityService planEligibilityService;

    @PostMapping("/{planId}/eligibility")
    @Operation(summary = "Assess plan eligibility", description = "Evaluates age and executes credit-rating or bad-cheque inquiries required by the active plan.")
    public AssessmentModels.Result assess(
            @PathVariable UUID planId,
            @Valid @RequestBody AssessmentRequest request
    ) {
        return planEligibilityService.assess(new AssessmentModels.Command(
                planId, request.nationalCode(), request.birthDate()
        ));
    }

    public record AssessmentRequest(@NotBlank String nationalCode, @NotNull LocalDate birthDate) {
    }
}
