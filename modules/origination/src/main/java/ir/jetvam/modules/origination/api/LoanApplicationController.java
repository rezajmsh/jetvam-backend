package ir.jetvam.modules.origination.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.infra.security.CurrentUser;
import ir.jetvam.modules.identity.service.CustomerProfileModels;
import ir.jetvam.modules.origination.service.LoanApplicationService;
import ir.jetvam.modules.origination.service.OriginationModels;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Exposes the authenticated customer's loan-application journey and enforces self ownership.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@RestController
@RequestMapping("/api/v1/origination/applications")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jetvam.origination.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Loan applications", description = "Customer journey from plan selection through contract signature.")
public class LoanApplicationController {

    private final LoanApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('origination:self:write')")
    @Operation(summary = "Create loan application", description = "Selects an active plan and snapshots its current commercial and workflow rules.")
    public OriginationModels.ApplicationView create(@Valid @RequestBody CreateApplicationRequest request) {
        return applicationService.create(currentPartyId(), new OriginationModels.CreateApplication(
                request.planId(), request.requestedAmount(), request.termMonths()
        ));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('origination:self:read')")
    @Operation(summary = "List my applications", description = "Returns only applications owned by the authenticated customer.")
    public List<OriginationModels.ApplicationView> mine() {
        return applicationService.findMine(currentPartyId());
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('origination:self:read')")
    @Operation(summary = "Get my application", description = "Returns the current stage, inquiry progress and payment obligations.")
    public OriginationModels.ApplicationView get(@PathVariable UUID applicationId) {
        return applicationService.getMine(currentPartyId(), applicationId);
    }

    @PostMapping("/{applicationId}/refresh")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('origination:self:write')")
    @Operation(summary = "Refresh application stage", description = "Applies confirmed fee payments and advances an eligible waiting stage.")
    public OriginationModels.ApplicationView refresh(@PathVariable UUID applicationId) {
        return applicationService.refresh(currentPartyId(), applicationId);
    }

    @PutMapping("/{applicationId}/personal-information")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('origination:self:write')")
    @Operation(summary = "Save personal and banking information", description = "Updates the reusable customer profile and attaches its revision to this application.")
    public OriginationModels.ApplicationView personal(
            @PathVariable UUID applicationId,
            @Valid @RequestBody PersonalInformationRequest request
    ) {
        return applicationService.savePersonalInformation(
                currentPartyId(), applicationId,
                new CustomerProfileModels.UpdatePersonalInformation(
                        request.bankCardNumber(), request.landline(), request.postalCode(), request.address()
                )
        );
    }

    @PutMapping("/{applicationId}/employment-information")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('origination:self:write')")
    @Operation(summary = "Save employment information", description = "Updates the reusable customer profile and attaches its revision to this application.")
    public OriginationModels.ApplicationView employment(
            @PathVariable UUID applicationId,
            @Valid @RequestBody EmploymentInformationRequest request
    ) {
        return applicationService.saveEmploymentInformation(
                currentPartyId(), applicationId,
                new CustomerProfileModels.UpdateEmploymentInformation(
                        request.educationCode(), request.employmentCode(), request.monthlyIncome(), request.documentIds()
                )
        );
    }

    @PutMapping("/{applicationId}/guarantee")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('origination:self:write')")
    @Operation(summary = "Save guarantee information", description = "Stores Sayad cheque and optional guarantor facts required by the selected plan.")
    public OriginationModels.ApplicationView guarantee(
            @PathVariable UUID applicationId,
            @Valid @RequestBody GuaranteeInformationRequest request
    ) {
        return applicationService.saveGuaranteeInformation(
                currentPartyId(), applicationId,
                new OriginationModels.GuaranteeInformation(
                        request.chequeSayadId(), request.bankCode(), request.chequeNumber(), request.chequeSerial(),
                        request.chequeDate(), request.chequeAmount(), request.guarantorNationalCode(),
                        request.guarantorMobile()
                )
        );
    }

    @PostMapping("/{applicationId}/contract-signature")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('origination:self:write')")
    @Operation(summary = "Confirm contract signature", description = "Advances a signature-ready application to credit allocation.")
    public OriginationModels.ApplicationView sign(@PathVariable UUID applicationId) {
        return applicationService.signContract(currentPartyId(), applicationId);
    }

    private static UUID currentPartyId() {
        return CurrentUser.partyId().orElseThrow(() -> new IllegalStateException("Customer party is missing"));
    }

    public record CreateApplicationRequest(
            @NotNull UUID planId,
            @NotNull @DecimalMin("0.01") BigDecimal requestedAmount,
            @Positive int termMonths
    ) {
    }

    public record PersonalInformationRequest(
            @NotBlank String bankCardNumber,
            @NotBlank String landline,
            @NotBlank String postalCode,
            @NotBlank String address
    ) {
    }

    public record EmploymentInformationRequest(
            @NotBlank String educationCode,
            @NotBlank String employmentCode,
            @NotNull @DecimalMin("0") BigDecimal monthlyIncome,
            List<UUID> documentIds
    ) {
    }

    public record GuaranteeInformationRequest(
            @NotBlank String chequeSayadId,
            @NotBlank String bankCode,
            @NotBlank String chequeNumber,
            @NotBlank String chequeSerial,
            @NotNull LocalDate chequeDate,
            @NotNull @DecimalMin("0.01") BigDecimal chequeAmount,
            String guarantorNationalCode,
            String guarantorMobile
    ) {
    }
}
