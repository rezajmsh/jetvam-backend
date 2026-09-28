package ir.jetvam.apps.uaa.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.common.security.UserCategory;
import ir.jetvam.infra.security.CurrentUser;
import ir.jetvam.modules.identity.service.CreateUserCommand;
import ir.jetvam.modules.identity.service.CreateAccountForPartyCommand;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import ir.jetvam.modules.identity.service.UserPage;
import ir.jetvam.modules.identity.service.UserSearchQuery;
import ir.jetvam.modules.identity.service.UserView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

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
@Validated
@Tag(name = "User administration", description = "Administrative account provisioning, lookup and lifecycle operations.")
public class UserManagementController {

    private final UaaUserManagementService userManagementService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'UAA_ADMIN') and hasAuthority('identity:user:write')")
    @Operation(summary = "Create a user", description = "Creates a password user and its individual party with the assigned roles and categories.")
    public UserView create(@Valid @RequestBody CreateUserRequest request) {
        return userManagementService.create(CurrentUser.userId(), new CreateUserCommand(
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
        return userManagementService.createForParty(CurrentUser.userId(), new CreateAccountForPartyCommand(
                partyId,
                request.username(),
                request.mobile(),
                request.password(),
                request.categories(),
                request.roles()
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR', 'UAA_ADMIN') and hasAuthority('identity:user:read')")
    @Operation(summary = "Search users", description = "Returns a bounded page filtered by text, status or category.")
    public UserPage search(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) UserAccountStatus status,
            @RequestParam(required = false) UserCategory category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return userManagementService.search(new UserSearchQuery(text, status, category, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR', 'UAA_ADMIN') and hasAuthority('identity:user:read')")
    @Operation(summary = "Get a user", description = "Returns an account by identifier for authorized identity operators.")
    public UserView get(@PathVariable UUID id) {
        return userManagementService.get(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'UAA_ADMIN') and hasAuthority('identity:user:write')")
    @Operation(summary = "Change user status", description = "Activates, disables or locks an account according to the requested lifecycle status.")
    public UserView changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeUserStatusRequest request
    ) {
        return userManagementService.changeStatus(CurrentUser.userId(), id, request.status());
    }

    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'UAA_ADMIN') and hasAuthority('identity:user:write')")
    @Operation(summary = "Unlock a user", description = "Clears a security lock and failed-attempt state without enabling a disabled account.")
    public UserView unlock(@PathVariable UUID id) {
        return userManagementService.unlock(CurrentUser.userId(), id);
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'UAA_ADMIN') and hasAuthority('identity:user:credential:reset')")
    @Operation(
            summary = "Reset a user password",
            description = "Replaces a password-account credential and revokes every renewable authorization."
    )
    public UserView resetPassword(
            @PathVariable UUID id,
            @Valid @RequestBody ResetUserPasswordRequest request
    ) {
        return userManagementService.resetPassword(CurrentUser.userId(), id, request.newPassword());
    }
}
