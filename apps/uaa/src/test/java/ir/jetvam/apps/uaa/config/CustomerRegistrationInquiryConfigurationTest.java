package ir.jetvam.apps.uaa.config;

import ir.jetvam.modules.inquiry.provider.GenericJsonMobileOwnershipAdapter;
import ir.jetvam.modules.inquiry.service.InquiryService;
import ir.jetvam.modules.integration.http.DynamicProviderHttpClientFactory;
import ir.jetvam.modules.integration.routing.ProviderRouter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies that UAA composes only the synchronous inquiry components required for registration.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
class CustomerRegistrationInquiryConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(CustomerRegistrationInquiryConfiguration.class)
            .withBean(ProviderRouter.class, () -> mock(ProviderRouter.class))
            .withBean(DynamicProviderHttpClientFactory.class, () -> mock(DynamicProviderHttpClientFactory.class));

    @Test
    void registersInquiryServiceAndMobileOwnershipAdapter() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(InquiryService.class);
            assertThat(context).hasSingleBean(GenericJsonMobileOwnershipAdapter.class);
        });
    }
}
