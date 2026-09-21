package ir.jetvam.common.exception;

/**
 * Stable machine-readable error contract shared between modules.
 */
public interface ErrorCode {

    String code();

    default String messageKey() {
        return "error." + code().toLowerCase().replace('.', '-');
    }
}
