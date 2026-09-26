package ir.jetvam.modules.payment.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.infra.security.CurrentUser;
import ir.jetvam.modules.payment.service.PaymentModels;
import ir.jetvam.modules.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Exposes customer fee checkout and trusted provider-confirmation endpoints.
 * A customer can create an attempt but cannot mark it successful.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jetvam.payment.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Payments", description = "Loan-application fees, checkout attempts and trusted confirmations.")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/applications/{applicationId}/fees")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('payment:self:read')")
    @Operation(summary = "List application fees", description = "Returns inquiry and application fees belonging to the authenticated customer.")
    public List<PaymentModels.FeeView> fees(@PathVariable UUID applicationId) {
        return paymentService.findFees(currentPartyId(), applicationId);
    }

    @PostMapping("/fees/{feeId}/attempts")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('payment:self:write')")
    @Operation(summary = "Create payment attempt", description = "Creates an idempotent checkout attempt for one pending fee.")
    public PaymentModels.AttemptView initiate(
            @PathVariable UUID feeId,
            @Valid @RequestBody InitiatePaymentRequest request
    ) {
        return paymentService.initiate(currentPartyId(), feeId, request.idempotencyKey());
    }

    @PostMapping("/attempts/{attemptId}/confirmation")
    @PreAuthorize("hasRole('SERVICE') and hasAuthority('payment:confirm')")
    @Operation(summary = "Confirm payment attempt", description = "Accepts a verified result from the payment-provider callback adapter.")
    public PaymentModels.AttemptView confirm(
            @PathVariable UUID attemptId,
            @Valid @RequestBody ConfirmPaymentRequest request
    ) {
        return paymentService.confirm(attemptId, request.successful(), request.providerReference());
    }

    private static UUID currentPartyId() {
        return CurrentUser.partyId().orElseThrow(() -> new IllegalStateException("Customer party is missing"));
    }

    public record InitiatePaymentRequest(@NotBlank String idempotencyKey) {
    }

    public record ConfirmPaymentRequest(boolean successful, String providerReference) {
    }
}
