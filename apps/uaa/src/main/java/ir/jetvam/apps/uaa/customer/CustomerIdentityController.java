package ir.jetvam.apps.uaa.customer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.modules.identity.service.CustomerAuthenticationService;
import ir.jetvam.modules.identity.service.CustomerRegistrationResult;
import ir.jetvam.modules.identity.service.CustomerRegistrationService;
import ir.jetvam.modules.identity.service.StartCustomerRegistrationCommand;
import ir.jetvam.modules.identity.service.VerifyCustomerRegistrationCommand;
import ir.jetvam.modules.otp.service.OtpChallengeView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes public customer registration and OTP authentication flows.
 * OAuth token issuance remains on the standard authorization-server endpoint.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
@Tag(name = "Customer identity", description = "Customer registration and OTP login.")
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

}
