package ir.jetvam.infra.web.error;

import ir.jetvam.common.exception.BusinessException;
import ir.jetvam.common.exception.ConflictException;
import ir.jetvam.common.exception.IllegalStateTransitionException;
import ir.jetvam.common.exception.IntegrationException;
import ir.jetvam.common.exception.OperationNotAllowedException;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.exception.RateLimitExceededException;
import ir.jetvam.common.exception.TechnicalException;
import ir.jetvam.common.exception.ValidationException;
import org.springframework.http.HttpStatus;

/**
 * Maps values for the default exception http status boundary.
 * Central mapping keeps framework and domain representations consistent.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public class DefaultExceptionHttpStatusMapper implements ExceptionHttpStatusMapper {

    @Override
    public HttpStatus statusFor(Throwable throwable) {
        return switch (throwable) {
            case ValidationException ignored -> HttpStatus.BAD_REQUEST;
            case ResourceNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case ConflictException ignored -> HttpStatus.CONFLICT;
            case RateLimitExceededException ignored -> HttpStatus.TOO_MANY_REQUESTS;
            case IllegalStateTransitionException ignored -> HttpStatus.CONFLICT;
            case OperationNotAllowedException ignored -> HttpStatus.FORBIDDEN;
            case IntegrationException ignored -> HttpStatus.BAD_GATEWAY;
            case BusinessException ignored -> HttpStatus.UNPROCESSABLE_CONTENT;
            case TechnicalException ignored -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
