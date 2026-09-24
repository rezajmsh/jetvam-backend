package ir.jetvam.apps.uaa.api;

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
public class SettingManagementController {

    private final SettingService settingService;

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and hasAuthority('settings:read')")
    public List<SettingView> findAll() {
        return settingService.findAll();
    }

    @GetMapping("/{key}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and hasAuthority('settings:read')")
    public SettingView get(@PathVariable String key) {
        return settingService.get(key);
    }

    @PatchMapping("/{key}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and hasAuthority('settings:write')")
    public SettingView update(@PathVariable String key, @Valid @RequestBody UpdateSettingRequest request) {
        return settingService.update(key, request.value());
    }
}
