package ir.jetvam.modules.payment.api;

import ir.jetvam.modules.payment.service.PaymentModels;
import ir.jetvam.modules.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes protected payment-gateway configuration for the operations back office.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@RestController
@RequestMapping("/api/v1/payments/gateways")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class PaymentGatewayAdminController {
    private final PaymentService paymentService;

    @GetMapping
    @PreAuthorize("hasAuthority('payment:configuration:read')")
    public List<PaymentModels.GatewayView> findAll() {
        return paymentService.findGateways();
    }

    @PutMapping("/{gatewayCode}")
    @PreAuthorize("hasAuthority('payment:configuration:write')")
    public PaymentModels.GatewayView update(
            @PathVariable String gatewayCode,
            @Valid @RequestBody UpdateGatewayRequest request
    ) {
        return paymentService.updateGateway(gatewayCode, request.title(), request.configurationJson(), request.active());
    }

    public record UpdateGatewayRequest(@NotBlank String title, String configurationJson, boolean active) {
    }
}
