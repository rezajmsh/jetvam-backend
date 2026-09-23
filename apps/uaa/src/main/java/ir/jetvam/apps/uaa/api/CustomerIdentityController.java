package ir.jetvam.apps.uaa.api;

import ir.jetvam.infra.security.CurrentUser;
import ir.jetvam.modules.identity.service.CompleteCustomerProfileCommand;
import ir.jetvam.modules.identity.service.CustomerAuthenticationService;
import ir.jetvam.modules.identity.service.CustomerProfileView;
import ir.jetvam.modules.identity.service.CustomerRegistrationResult;
import ir.jetvam.modules.identity.service.CustomerRegistrationService;
import ir.jetvam.modules.identity.service.OtpChallengeView;
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
public class CustomerIdentityController {

    private final CustomerRegistrationService registrationService;
    private final CustomerAuthenticationService authenticationService;

    @PostMapping("/registrations/otp")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OtpChallengeView requestRegistrationOtp(@Valid @RequestBody StartCustomerRegistrationRequest request) {
        return registrationService.requestOtp(new StartCustomerRegistrationCommand(
                request.mobile(),
                request.nationalCode()
        ));
    }

    @PostMapping("/registrations/verify")
    @ResponseStatus(HttpStatus.CREATED)
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
    public OtpChallengeView requestLoginOtp(@Valid @RequestBody RequestCustomerLoginOtp request) {
        return authenticationService.requestOtp(request.mobile());
    }

    @PatchMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerProfileView completeProfile(@Valid @RequestBody CompleteCustomerProfileRequest request) {
        return registrationService.completeProfile(
                CurrentUser.userId(),
                new CompleteCustomerProfileCommand(request.firstName(), request.lastName(), request.birthDate())
        );
    }
}
