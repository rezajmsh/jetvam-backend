package ir.jetvam.apps.uaa.settings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.modules.settings.service.SettingService;
import ir.jetvam.modules.settings.service.SettingView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Administrative API for runtime settings, including password-user 2FA policies.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "Runtime settings", description = "Runtime policies such as password-user two-factor authentication requirements.")
public class SettingManagementController {

    private final SettingService settingService;

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and hasAuthority('settings:read')")
    @Operation(summary = "List settings", description = "Returns all runtime settings visible to system administrators.")
    public List<SettingView> findAll() {
        return settingService.findAll();
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and hasAuthority('settings:read')")
    @Operation(summary = "Get a setting", description = "Returns the current typed value and metadata for one setting key.")
    public SettingView get(@PathVariable String key) {
        return settingService.get(key);
    }

    @PatchMapping("/{key}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and hasAuthority('settings:write')")
    @Operation(summary = "Update a setting", description = "Validates and changes one runtime setting without an application deployment.")
    public SettingView update(@PathVariable String key, @Valid @RequestBody UpdateSettingRequest request) {
        return settingService.update(key, request.value());
    }
}
