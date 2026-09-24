package ir.jetvam.apps.uaa.api;

import ir.jetvam.modules.identity.service.PasswordUserAuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Starts password login and sends the second factor when its category policy requires it.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
@RestController
@RequestMapping("/api/v1/password-users/auth")
@RequiredArgsConstructor
public class PasswordIdentityController {

    private final PasswordUserAuthenticationService authenticationService;

    @PostMapping("/prepare")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PasswordLoginPreparationView prepare(@Valid @RequestBody PreparePasswordLoginRequest request) {
        return PasswordLoginPreparationView.from(
                authenticationService.prepare(request.username(), request.password())
        );
    }
}
