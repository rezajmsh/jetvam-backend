package ir.jetvam.modules.integration.routing;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Prevents provider network calls while a database transaction is active on the current thread.
 * This runtime invariant protects connection pools and database locks from external latency.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public final class ExternalCallTransactionGuard {

    private ExternalCallTransactionGuard() {
    }

    public static void assertNoActiveTransaction(String capabilityCode) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                    "External capability " + capabilityCode + " cannot be called inside a database transaction"
            );
        }
    }
}
