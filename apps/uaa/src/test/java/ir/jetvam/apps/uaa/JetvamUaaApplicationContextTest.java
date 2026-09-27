package ir.jetvam.apps.uaa;

import ir.jetvam.modules.inquiry.service.PersistentInquiryService;
import ir.jetvam.modules.settings.api.SettingManagementController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the composed UAA application can wire shared module entry points.
 * The test protects Jackson integration and module-owned settings API discovery.
 *
 * @author reza jamshidi
 * @since 9/27/2026
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "jetvam.persist.url=jdbc:h2:mem:jetvam_uaa_context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
class JetvamUaaApplicationContextTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void wiresJacksonThreeConsumersAndModuleOwnedSettingsController() {
        assertThat(context.getBean(ObjectMapper.class)).isNotNull();
        assertThat(context.getBean(PersistentInquiryService.class)).isNotNull();
        assertThat(context.getBean(SettingManagementController.class)).isNotNull();
    }
}
