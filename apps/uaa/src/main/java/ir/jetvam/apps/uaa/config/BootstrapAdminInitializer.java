package ir.jetvam.apps.uaa.config;

import ir.jetvam.common.security.UserCategory;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.IdentityRoles;
import ir.jetvam.modules.identity.service.CreateUserCommand;
import ir.jetvam.modules.identity.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Creates the first UAA administrator when explicitly enabled by deployment settings.
 * The initializer is idempotent and never replaces an existing account or password.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Component
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final JetvamUaaProperties properties;
    private final UserAccountService userAccountService;

    @Override
    public void run(ApplicationArguments args) {
        JetvamUaaProperties.BootstrapAdmin admin = properties.getBootstrapAdmin();
        if (!admin.isEnabled()) {
            return;
        }
        String username = Preconditions.requireText(admin.getUsername(), "jetvam.uaa.bootstrap-admin.username");
        if (userAccountService.usernameExists(username)) {
            return;
        }
        userAccountService.create(new CreateUserCommand(
                username,
                admin.getMobile(),
                admin.getNationalCode(),
                admin.getFirstName(),
                admin.getLastName(),
                null,
                admin.getPassword(),
                Set.of(UserCategory.OPERATOR),
                Set.of(IdentityRoles.SYSTEM_ADMIN)
        ));
    }
}
