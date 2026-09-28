package ir.jetvam.apps.uaa.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.infra.security.CurrentUser;
import ir.jetvam.modules.identity.service.UserView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes authenticated self-service account operations from the UAA boundary.
 * Customer OTP accounts may read their account but cannot use password operations.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Tag(name = "Current account", description = "Authenticated account details and credential maintenance.")
public class AccountController {

    private final CurrentAccountService currentAccountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SYSTEM_ADMIN', 'SYSTEM_OPERATOR', 'UAA_ADMIN', "
            + "'MERCHANT_ADMIN', 'MERCHANT_OPERATOR', 'MERCHANT_USER') "
            + "and hasAuthority('identity:account:read:self')")
    @Operation(summary = "Get current account", description = "Returns the account represented by the access token.")
    public UserView current() {
        return currentAccountService.get(CurrentUser.userId());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR', 'UAA_ADMIN', "
            + "'MERCHANT_ADMIN', 'MERCHANT_OPERATOR', 'MERCHANT_USER') "
            + "and hasAuthority('identity:credential:write:self')")
    @Operation(
            summary = "Change my password",
            description = "Verifies the current password, replaces it and revokes every renewable authorization."
    )
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        currentAccountService.changePassword(
                CurrentUser.userId(),
                request.currentPassword(),
                request.newPassword()
        );
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SYSTEM_ADMIN', 'SYSTEM_OPERATOR', 'UAA_ADMIN', "
            + "'MERCHANT_ADMIN', 'MERCHANT_OPERATOR', 'MERCHANT_USER') "
            + "and hasAuthority('identity:session:revoke:self')")
    @Operation(
            summary = "Log out current account",
            description = "Revokes every renewable authorization owned by the authenticated account."
    )
    public void logout() {
        currentAccountService.logout(CurrentUser.userId());
    }
}
