package ir.jetvam.modules.origination.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.modules.origination.service.LoanApplicationService;
import ir.jetvam.modules.origination.service.OriginationModels;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

/**
 * Exposes controlled operational actions that cannot be performed by the customer frontend.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@RestController
@RequestMapping("/api/v1/origination/operations/applications")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jetvam.origination.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR') and hasAuthority('origination:manage')")
@Tag(name = "Origination operations", description = "Operational confirmation of physical cheque delivery and credit allocation.")
public class OriginationOperationsController {

    private final LoanApplicationService applicationService;

    @GetMapping
    @Operation(summary = "List applications for operations")
    public List<OriginationModels.ApplicationView> findAll() {
        return applicationService.findAllForOperations();
    }

    @PostMapping("/{applicationId}/original-collaterals-received")
    @Operation(summary = "Confirm original collaterals", description = "Records operational receipt of every required physical collateral for the application.")
    public OriginationModels.ApplicationView originalCollaterals(@PathVariable UUID applicationId) {
        return applicationService.markOriginalCollateralsReceived(applicationId);
    }

    @PostMapping("/{applicationId}/credit-allocated")
    @Operation(summary = "Confirm credit allocation", description = "Completes an application after downstream credit allocation succeeds.")
    public OriginationModels.ApplicationView allocate(@PathVariable UUID applicationId) {
        return applicationService.allocateCredit(applicationId);
    }
}
