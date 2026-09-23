package ir.jetvam.common.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the behavior of preconditions.
 * The tests protect the shared contract and its important edge cases.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

class PreconditionsTest {

    @Test
    void returnsValidValuesForInlineUse() {
        assertEquals("value", Preconditions.requireText("value", "name"));
        assertEquals("value", Preconditions.requireNonNull("value", "name"));
        assertEquals(3, Preconditions.requirePositive(3, "count"));
        assertEquals(0, Preconditions.requireNonNegative(0, "offset"));
        assertEquals(5, Preconditions.requireInRange(5, 1, 10, "pageSize"));
    }

    @Test
    void failsFastWithConsistentArgumentMessages() {
        IllegalArgumentException blank = assertThrows(
                IllegalArgumentException.class,
                () -> Preconditions.requireText("  ", "jetvam.persist.url")
        );
        assertEquals("jetvam.persist.url must not be blank", blank.getMessage());

        IllegalArgumentException missing = assertThrows(
                IllegalArgumentException.class,
                () -> Preconditions.requireNonNull(null, "clock")
        );
        assertEquals("clock must not be null", missing.getMessage());
    }
}
