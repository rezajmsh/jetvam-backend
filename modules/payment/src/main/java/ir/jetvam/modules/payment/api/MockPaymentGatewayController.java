package ir.jetvam.modules.payment.api;

import ir.jetvam.modules.payment.service.PaymentModels;
import ir.jetvam.modules.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Provides a browser redirect and signed callback simulator with the same boundary as a real IPG adapter.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@RestController
@RequestMapping("/api/v1/payments/mock")
@RequiredArgsConstructor
public class MockPaymentGatewayController {
    private final PaymentService paymentService;

    @GetMapping(value = "/{attemptId}", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("permitAll()")
    public String page(@PathVariable UUID attemptId, @RequestParam String token) {
        String action = "/api/v1/payments/mock/" + attemptId + "/result?token=" + token;
        return """
                <!doctype html><html lang="fa" dir="rtl"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1"><title>درگاه آزمایشی جت‌وام</title>
                <style>body{font-family:Tahoma;background:#f5f6f8;display:grid;place-items:center;min-height:100vh;margin:0}.card{background:#fff;padding:32px;border-radius:22px;box-shadow:0 20px 60px #18223020;width:min(420px,85vw)}h1{color:#101828}button{border:0;padding:13px 20px;border-radius:12px;margin:8px;font-weight:700;cursor:pointer}.ok{background:#c8102e;color:white}.fail{background:#eee;color:#333}</style></head>
                <body><div class="card"><h1>درگاه آزمایشی جت‌وام</h1><p>این صفحه رفتار redirect و callback درگاه واقعی را شبیه‌سازی می‌کند.</p>
                <form method="post" action="%s"><button class="ok" name="successful" value="true">پرداخت موفق</button><button class="fail" name="successful" value="false">پرداخت ناموفق</button></form></div></body></html>
                """.formatted(action);
    }

    @PostMapping("/{attemptId}/result")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> result(
            @PathVariable UUID attemptId,
            @RequestParam String token,
            @RequestParam boolean successful
    ) {
        PaymentModels.AttemptView result = paymentService.completeMock(attemptId, token, successful);
        String separator = result.returnUrl().contains("?") ? "&" : "?";
        URI location = URI.create(result.returnUrl() + separator + "payment=" + (successful ? "success" : "failed"));
        return ResponseEntity.status(303).header(HttpHeaders.LOCATION, location.toString()).build();
    }
}
