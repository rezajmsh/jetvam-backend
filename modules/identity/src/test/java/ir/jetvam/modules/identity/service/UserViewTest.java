package ir.jetvam.modules.identity.service;

import ir.jetvam.common.security.UserCategory;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.model.UserAccountStatus;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies that user API views never retain mutable or persistence-backed collections.
 * Detached views must remain serializable after the account transaction has completed.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
class UserViewTest {

    @Test
    void createsImmutableSnapshotsOfCategoriesAndRoles() {
        Set<UserCategory> categories = new LinkedHashSet<>(Set.of(UserCategory.CUSTOMER));
        Set<String> roles = new LinkedHashSet<>(Set.of("CUSTOMER"));

        UserView view = new UserView(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Customer",
                null,
                "09121234567",
                AuthenticationMethod.OTP,
                UserAccountStatus.ACTIVE,
                categories,
                roles
        );
        categories.clear();
        roles.clear();

        assertThat(view.categories()).containsExactly(UserCategory.CUSTOMER);
        assertThat(view.roles()).containsExactly("CUSTOMER");
        assertThatThrownBy(() -> view.categories().add(UserCategory.OPERATOR))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> view.roles().add("SYSTEM_OPERATOR"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
