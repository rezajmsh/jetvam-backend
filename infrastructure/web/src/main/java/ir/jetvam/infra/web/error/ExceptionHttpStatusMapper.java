package ir.jetvam.infra.web.error;

import org.springframework.http.HttpStatus;

/**
 * Maps values for the exception http status boundary.
 * Central mapping keeps framework and domain representations consistent.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public interface ExceptionHttpStatusMapper {

    HttpStatus statusFor(Throwable throwable);
}
