package ir.jetvam.apps.uaa.user;

import ir.jetvam.apps.uaa.JetvamUaaApplication;
import ir.jetvam.common.security.UserCategory;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Verifies that administrative account searches support absent filters and category membership.
 * The test exercises the generated persistence query instead of mocking repository behavior.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
@SpringBootTest(
        classes = JetvamUaaApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jetvam.persist.url=jdbc:h2:mem:user_account_search;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "jetvam.persist.driver-class-name=org.h2.Driver",
                "jetvam.persist.username=sa",
                "jetvam.persist.password=",
                "jetvam.persist.default-schema=PUBLIC",
                "jetvam.persist.jpa.ddl-auto=create-drop",
                "jetvam.persist.migration.enabled=false",
                "jetvam.notification.dispatcher.enabled=false",
                "jetvam.i18n.enabled=false",
                "jetvam.uaa.bootstrap-admin.enabled=false",
                "jetvam.uaa.signing-key.allow-ephemeral=true"
        }
)
class UserAccountRepositorySearchTest {

    @Autowired
    private UserAccountRepository repository;

    @Test
    void searchesWithoutOptionalFilters() {
        assertThatNoException().isThrownBy(() ->
                repository.search(null, null, null, PageRequest.of(0, 20)));
    }

    @Test
    void filtersByCustomerCategory() {
        assertThatNoException().isThrownBy(() ->
                repository.search(null, null, UserCategory.CUSTOMER, PageRequest.of(0, 20)));
    }
}
