package ir.jetvam.common.exception;

/**
 * Stable machine-readable error contract shared between modules.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public interface ErrorCode {

    String code();

    default String messageKey() {
        return "error." + code().toLowerCase().replace('.', '-');
    }
}
