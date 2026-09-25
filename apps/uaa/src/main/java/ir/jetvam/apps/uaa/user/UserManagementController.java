package ir.jetvam.apps.uaa.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.infra.security.CurrentUser;
import ir.jetvam.modules.identity.service.CreateUserCommand;
import ir.jetvam.modules.identity.service.CreateAccountForPartyCommand;
import ir.jetvam.modules.identity.service.UserAccountService;
import ir.jetvam.modules.identity.service.UserView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Exposes secured administrative operations for account provisioning and lifecycle.
 * API responses are automatically wrapped by the shared web infrastructure.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User administration", description = "Administrative account provisioning, lookup and lifecycle operations.")
public class UserManagementController {

    private final UserAccountService userAccountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'UAA_ADMIN') and hasAuthority('identity:user:write')")
    @Operation(summary = "Create a user", description = "Creates a password user and its individual party with the assigned roles and categories.")
    public UserView create(@Valid @RequestBody CreateUserRequest request) {
        return userAccountService.create(new CreateUserCommand(
                request.username(),
                request.mobile(),
                request.nationalCode(),
                request.firstName(),
                request.lastName(),
                request.birthDate(),
                request.password(),
                request.categories(),
                request.roles()
        ));
    }

    @PostMapping("/parties/{partyId}/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'UAA_ADMIN') and hasAuthority('identity:user:write')")
    @Operation(summary = "Create an account for a party", description = "Adds a password account to an existing party without duplicating party identity data.")
    public UserView createForParty(
            @PathVariable UUID partyId,
            @Valid @RequestBody CreateAccountForPartyRequest request
    ) {
        return userAccountService.createForParty(new CreateAccountForPartyCommand(
                partyId,
                request.username(),
                request.mobile(),
                request.password(),
                request.categories(),
                request.roles()
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR', 'UAA_ADMIN') and hasAuthority('identity:user:read')")
    @Operation(summary = "Get a user", description = "Returns an account by identifier for authorized identity operators.")
    public UserView get(@PathVariable UUID id) {
        return userAccountService.get(id);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user", description = "Returns the account represented by the current access token.")
    public UserView current() {
        return userAccountService.get(CurrentUser.userId());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'UAA_ADMIN') and hasAuthority('identity:user:write')")
    @Operation(summary = "Change user status", description = "Activates, disables or locks an account according to the requested lifecycle status.")
    public UserView changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeUserStatusRequest request
    ) {
        return userAccountService.changeStatus(id, request.status());
    }
}
