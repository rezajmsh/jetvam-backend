package ir.jetvam.modules.origination.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.modules.origination.service.LoanApplicationService;
import ir.jetvam.modules.origination.service.OriginationModels;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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

    @PostMapping("/{applicationId}/original-cheque-received")
    @Operation(summary = "Confirm original cheque", description = "Records operational receipt and validation of the original guarantee cheque.")
    public OriginationModels.ApplicationView originalCheque(@PathVariable UUID applicationId) {
        return applicationService.markOriginalChequeReceived(applicationId);
    }

    @PostMapping("/{applicationId}/credit-allocated")
    @Operation(summary = "Confirm credit allocation", description = "Completes an application after downstream credit allocation succeeds.")
    public OriginationModels.ApplicationView allocate(@PathVariable UUID applicationId) {
        return applicationService.allocateCredit(applicationId);
    }
}
