package ir.jetvam.common.exception;

import java.util.Map;

/**
 * Represents a illegal state transition failure in the common exception hierarchy.
 * Its stable error code can be mapped consistently at API boundaries.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public final class IllegalStateTransitionException extends BusinessException {

    public IllegalStateTransitionException(Object from, Object to) {
        super(
                CommonErrorCode.ILLEGAL_STATE_TRANSITION,
                "State transition from %s to %s is not allowed".formatted(from, to),
                Map.of("from", String.valueOf(from), "to", String.valueOf(to))
        );
    }
}
