package ir.jetvam.apps.uaa.config;

import ir.jetvam.modules.inquiry.provider.GenericJsonMobileOwnershipAdapter;
import ir.jetvam.modules.inquiry.provider.MockMobileOwnershipAdapter;
import ir.jetvam.modules.inquiry.service.RoutingInquiryService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Composes the synchronous mobile-ownership inquiry required by customer registration.
 * Other inquiry APIs and asynchronous workers remain outside the UAA application.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Configuration(proxyBeanMethods = false)
@Import({
        RoutingInquiryService.class,
        GenericJsonMobileOwnershipAdapter.class,
        MockMobileOwnershipAdapter.class
})
public class CustomerRegistrationInquiryConfiguration {
}
