package ir.jetvam.common.validation;

import ir.jetvam.common.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the behavior of validation collector.
 * The tests protect the shared contract and its important edge cases.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

class ValidationCollectorTest {

    @Test
    void reportsAllViolationsAtOnce() {
        ValidationCollector collector = ValidationCollector.create()
                .requireText(" ", "mobile")
                .require(false, "nationalCode", "INVALID", "national code is invalid");

        ValidationException exception = assertThrows(ValidationException.class, collector::throwIfInvalid);

        assertEquals(2, exception.violations().size());
        assertEquals(2, exception.details().get("violationCount"));
    }
}
