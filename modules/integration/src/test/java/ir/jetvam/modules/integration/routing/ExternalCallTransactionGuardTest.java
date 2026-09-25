package ir.jetvam.modules.integration.routing;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the invariant that provider calls cannot run within database transactions.
 * The guard is shared by every capability routed through the integration module.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
class ExternalCallTransactionGuardTest {

    @Test
    void allowsCallsWithoutTransaction() {
        assertThatCode(() -> ExternalCallTransactionGuard.assertNoActiveTransaction("SHAHKAR"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsCallsInsideTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThatThrownBy(() -> ExternalCallTransactionGuard.assertNoActiveTransaction("SHAHKAR"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SHAHKAR")
                    .hasMessageContaining("database transaction");
        } finally {
            TransactionSynchronizationManager.clear();
        }
    }
}
