package ir.jetvam.apps.services.inquiry;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.modules.inquiry.service.InquiryDefinitionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * Manages inquiry validity and enablement without changing provider or application code.
 * Only system administrators may mutate these runtime business settings.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
@RestController
@RequestMapping("/api/v1/inquiries/definitions")
@RequiredArgsConstructor
@Tag(name = "Inquiry configuration", description = "Runtime validity policy for inquiry capabilities.")
public class InquiryDefinitionController {

    private final InquiryDefinitionService definitionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR') and hasAuthority('inquiry:configuration:read')")
    @Operation(summary = "List inquiry definitions")
    public List<InquiryDefinitionService.DefinitionView> findAll() {
        return definitionService.findAll();
    }

    @PutMapping("/{inquiryCode}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and hasAuthority('inquiry:configuration:write')")
    @Operation(summary = "Update inquiry validity and enablement")
    public InquiryDefinitionService.DefinitionView update(
            @PathVariable String inquiryCode,
            @Valid @RequestBody UpdateInquiryDefinitionRequest request
    ) {
        return definitionService.update(
                inquiryCode,
                Duration.ofSeconds(request.validitySeconds()),
                request.enabled()
        );
    }

    public record UpdateInquiryDefinitionRequest(
            @NotNull @PositiveOrZero Long validitySeconds,
            @NotNull Boolean enabled
    ) {
    }
}
