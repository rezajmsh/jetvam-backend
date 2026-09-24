package ir.jetvam.modules.integration.shahkar;

import ir.jetvam.common.exception.IntegrationException;
import ir.jetvam.modules.integration.IntegrationCapabilities;
import ir.jetvam.modules.integration.routing.ProviderRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Provides a consumer-independent Shahkar facade backed by runtime provider routing.
 * Identity and future modules share this implementation without owning transport code.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Component
@RequiredArgsConstructor
public class RoutingShahkarProvider implements ShahkarProvider {

    private final ProviderRouter providerRouter;

    @Override
    public ShahkarVerification verify(String mobile, String nationalCode) {
        try {
            return providerRouter.execute(
                    IntegrationCapabilities.SHAHKAR_VERIFY,
                    new ShahkarCommand(mobile, nationalCode),
                    ShahkarVerification.class,
                    true
            );
        } catch (RuntimeException exception) {
            throw new IntegrationException("shahkar", "verify", exception);
        }
    }
}
