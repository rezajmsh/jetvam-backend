package ir.jetvam.common.exception;

import java.util.Map;

public final class IllegalStateTransitionException extends BusinessException {

    public IllegalStateTransitionException(Object from, Object to) {
        super(
                CommonErrorCode.ILLEGAL_STATE_TRANSITION,
                "State transition from %s to %s is not allowed".formatted(from, to),
                Map.of("from", String.valueOf(from), "to", String.valueOf(to))
        );
    }
}
