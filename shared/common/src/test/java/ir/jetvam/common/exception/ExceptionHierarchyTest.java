package ir.jetvam.common.exception;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the behavior of exception hierarchy.
 * The tests protect the shared contract and its important edge cases.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

class ExceptionHierarchyTest {

    @Test
    void separatesBusinessAndTechnicalFailures() {
        JetvamException business = new ResourceNotFoundException("LoanRequest", 42L);
        JetvamException technical = new IntegrationException("core-banking", "createFacility", new RuntimeException());

        assertInstanceOf(BusinessException.class, business);
        assertInstanceOf(TechnicalException.class, technical);
        assertEquals("COMMON.RESOURCE_NOT_FOUND", business.code());
        assertEquals("core-banking", technical.details().get("provider"));
    }

    @Test
    void protectsExceptionDetailsFromMutation() {
        ConflictException exception = new ConflictException("duplicate", Map.of("key", "value"));
        assertThrows(UnsupportedOperationException.class, () -> exception.details().put("other", "value"));
    }
}
