package ir.jetvam.modules.identity.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.infra.security.CurrentUser;
import ir.jetvam.modules.identity.service.CompleteCustomerProfileCommand;
import ir.jetvam.modules.identity.service.CustomerProfileCompletionService;
import ir.jetvam.modules.identity.service.CustomerProfileDataService;
import ir.jetvam.modules.identity.service.CustomerProfileModels;
import ir.jetvam.modules.identity.service.CustomerProfileView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Exposes self-service customer profile reads and independent personal or employment updates.
 * The authenticated party identifier is the only ownership source accepted by this API.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@RestController
@RequestMapping("/api/v1/customer/profile")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jetvam.identity.profile-api", name = "enabled", havingValue = "true")
@PreAuthorize("hasRole('CUSTOMER')")
@Tag(name = "Customer profile", description = "Reusable personal and employment information owned by the customer.")
public class CustomerProfileController {

    private final CustomerProfileDataService profileService;
    private final CustomerProfileCompletionService profileCompletionService;

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('profile:read:self')")
    @Operation(summary = "Get my profile", description = "Returns reusable personal and employment data and their current revisions.")
    public CustomerProfileModels.ProfileView get() {
        return profileService.get(currentPartyId());
    }

    @PutMapping("/identity-information")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('profile:write:self')")
    @Operation(summary = "Complete my identity information", description = "Completes the verified customer's name and birth date after registration.")
    public CustomerProfileView completeIdentity(@Valid @RequestBody IdentityInformationRequest request) {
        return profileCompletionService.completeIdentity(
                CurrentUser.userId(),
                new CompleteCustomerProfileCommand(request.firstName(), request.lastName(), request.birthDate())
        );
    }

    @PutMapping("/personal-information")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('profile:write:self')")
    @Operation(summary = "Update my personal information", description = "Updates reusable banking, contact and address information independently of an application.")
    public CustomerProfileModels.ProfileView updatePersonal(@Valid @RequestBody PersonalInformationRequest request) {
        return profileService.updatePersonalInformation(
                currentPartyId(),
                new CustomerProfileModels.UpdatePersonalInformation(
                        request.bankCardNumber(), request.landline(), request.postalCode(), request.address()
                )
        );
    }

    @PutMapping("/employment-information")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('profile:write:self')")
    @Operation(summary = "Update my employment information", description = "Updates reusable education, employment, income and document references independently of an application.")
    public CustomerProfileModels.ProfileView updateEmployment(
            @Valid @RequestBody EmploymentInformationRequest request
    ) {
        return profileService.updateEmploymentInformation(
                currentPartyId(),
                new CustomerProfileModels.UpdateEmploymentInformation(
                        request.educationCode(), request.employmentCode(), request.monthlyIncome(), request.documentIds()
                )
        );
    }

    private static UUID currentPartyId() {
        return CurrentUser.partyId().orElseThrow(() -> new IllegalStateException("Customer party is missing"));
    }

    public record PersonalInformationRequest(
            @NotBlank @Pattern(regexp = "\\d{16}") String bankCardNumber,
            @NotBlank @Size(max = 20) String landline,
            @NotBlank @Pattern(regexp = "\\d{10}") String postalCode,
            @NotBlank @Size(max = 1000) String address
    ) {
    }

    public record IdentityInformationRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotNull LocalDate birthDate
    ) {
    }

    public record EmploymentInformationRequest(
            @NotBlank @Size(max = 80) String educationCode,
            @NotBlank @Size(max = 80) String employmentCode,
            @NotNull @DecimalMin("0") BigDecimal monthlyIncome,
            @Size(max = 20) List<@NotNull UUID> documentIds
    ) {
    }
}
