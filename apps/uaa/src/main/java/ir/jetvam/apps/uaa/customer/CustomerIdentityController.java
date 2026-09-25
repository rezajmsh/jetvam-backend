package ir.jetvam.apps.uaa.customer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.infra.security.CurrentUser;
import ir.jetvam.modules.identity.service.CompleteCustomerProfileCommand;
import ir.jetvam.modules.identity.service.CustomerAuthenticationService;
import ir.jetvam.modules.identity.service.CustomerProfileView;
import ir.jetvam.modules.identity.service.CustomerRegistrationResult;
import ir.jetvam.modules.identity.service.CustomerRegistrationService;
import ir.jetvam.modules.otp.service.OtpChallengeView;
import ir.jetvam.modules.identity.service.StartCustomerRegistrationCommand;
import ir.jetvam.modules.identity.service.VerifyCustomerRegistrationCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes public customer OTP flows and authenticated profile completion.
 * OAuth token issuance remains on the standard authorization-server endpoint.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
@Tag(name = "Customer identity", description = "Customer registration, OTP login and profile completion.")
public class CustomerIdentityController {

    private final CustomerRegistrationService registrationService;
    private final CustomerAuthenticationService authenticationService;

    @PostMapping("/registrations/otp")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Start customer registration", description = "Validates mobile and national code, then sends a purpose-bound registration OTP.")
    public OtpChallengeView requestRegistrationOtp(@Valid @RequestBody StartCustomerRegistrationRequest request) {
        return registrationService.requestOtp(new StartCustomerRegistrationCommand(
                request.mobile(),
                request.nationalCode()
        ));
    }

    @PostMapping("/registrations/verify")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Verify customer registration", description = "Verifies OTP and Shahkar ownership, then creates the passwordless customer account.")
    public CustomerRegistrationResult verifyRegistrationOtp(
            @Valid @RequestBody VerifyCustomerRegistrationRequest request
    ) {
        return registrationService.verifyOtp(new VerifyCustomerRegistrationCommand(
                request.challengeId(),
                request.otp()
        ));
    }

    @PostMapping("/auth/otp")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Request customer login OTP", description = "Sends the OTP used by the customer OAuth grant to obtain tokens.")
    public OtpChallengeView requestLoginOtp(@Valid @RequestBody RequestCustomerLoginOtp request) {
        return authenticationService.requestOtp(request.mobile());
    }

    @PatchMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER') and hasAuthority('profile:write:self')")
    @Operation(summary = "Complete customer profile", description = "Completes the authenticated customer's identity profile after registration.")
    public CustomerProfileView completeProfile(@Valid @RequestBody CompleteCustomerProfileRequest request) {
        return registrationService.completeProfile(
                CurrentUser.userId(),
                new CompleteCustomerProfileCommand(request.firstName(), request.lastName(), request.birthDate())
        );
    }
}
