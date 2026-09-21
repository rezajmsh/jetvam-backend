package ir.jetvam.infra.web.error;

import ir.jetvam.common.exception.CommonErrorCode;
import ir.jetvam.common.exception.FieldViolation;
import ir.jetvam.common.exception.JetvamException;
import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.infra.observability.logging.OtelEventLogger;
import ir.jetvam.infra.observability.logging.OtelEventType;
import ir.jetvam.infra.observability.config.JetvamObservabilityProperties;
import ir.jetvam.infra.web.api.ApiError;
import ir.jetvam.infra.web.api.ApiResponse;
import ir.jetvam.infra.web.api.ApiResponseFactory;
import ir.jetvam.infra.web.config.JetvamWebProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.ErrorResponseException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("jetvam.http.error");
    private static final String INVALID_REQUEST = "WEB.INVALID_REQUEST";
    private static final String INTERNAL_ERROR = "WEB.INTERNAL_ERROR";

    private final ExceptionHttpStatusMapper statusMapper;
    private final ApiResponseFactory responseFactory;
    private final ObjectProvider<MessageSource> messageSourceProvider;
    private final JetvamWebProperties properties;
    private final JetvamObservabilityProperties observabilityProperties;
    private final OtelEventLogger eventLogger;

    @ExceptionHandler(JetvamException.class)
    public ResponseEntity<ApiResponse<Void>> handleJetvamException(
            JetvamException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = statusMapper.statusFor(exception);
        List<FieldViolation> violations = exception instanceof ValidationException validation
                ? validation.violations()
                : List.of();
        Map<String, Object> details = properties.getErrors().isIncludeDetails()
                ? exception.details()
                : Map.of();
        return response(exception, exception.code(), localize(exception), violations, details, status, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidArguments(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(this::fieldViolation)
                .toList();
        return response(exception, CommonErrorCode.VALIDATION_FAILED.code(), "Request validation failed",
                violations, Map.of(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolations(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<FieldViolation> violations = exception.getConstraintViolations().stream()
                .map(violation -> new FieldViolation(
                        violation.getPropertyPath().toString(),
                        "VALIDATION.CONSTRAINT",
                        violation.getMessage()
                ))
                .toList();
        return response(exception, CommonErrorCode.VALIDATION_FAILED.code(), "Request validation failed",
                violations, Map.of(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception exception, HttpServletRequest request) {
        return response(exception, INVALID_REQUEST, "Request is invalid", List.of(), Map.of(),
                HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(Exception exception, HttpServletRequest request) {
        return response(exception, "WEB.RESOURCE_NOT_FOUND", "Resource was not found", List.of(), Map.of(),
                HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(Exception exception, HttpServletRequest request) {
        return response(exception, "WEB.METHOD_NOT_ALLOWED", "HTTP method is not allowed", List.of(), Map.of(),
                HttpStatus.METHOD_NOT_ALLOWED, request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(
            Exception exception,
            HttpServletRequest request
    ) {
        return response(exception, "WEB.UNSUPPORTED_MEDIA_TYPE", "Media type is not supported", List.of(), Map.of(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotAcceptable(Exception exception, HttpServletRequest request) {
        return response(exception, "WEB.NOT_ACCEPTABLE", "Requested response type is not available", List.of(),
                Map.of(), HttpStatus.NOT_ACCEPTABLE, request);
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiResponse<Void>> handleSpringErrorResponse(
            ErrorResponseException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return response(exception, "WEB.HTTP_ERROR", "Request could not be processed", List.of(), Map.of(),
                status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception, HttpServletRequest request) {
        return response(exception, INTERNAL_ERROR, "An unexpected error occurred", List.of(), Map.of(),
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ApiResponse<Void>> response(
            Exception exception,
            String code,
            String message,
            List<FieldViolation> violations,
            Map<String, Object> details,
            HttpStatus status,
            HttpServletRequest request
    ) {
        if (observabilityProperties.isEnabled() && observabilityProperties.getLogging().isEnabled()) {
            Level level = status.is5xxServerError() ? Level.ERROR : Level.WARN;
            eventLogger.log(
                    LOGGER,
                    level,
                    "http.server.request.error",
                    OtelEventType.HTTP_SERVER_REQUEST,
                    "HTTP request failed",
                    eventLogger.attributes(
                            "error.type", exception.getClass().getName(),
                            "error.code", code,
                            "http.request.method", request.getMethod(),
                            "url.path", request.getRequestURI(),
                            "http.response.status_code", status.value()
                    ),
                    status.is5xxServerError() ? exception : null
            );
        }
        ApiError error = new ApiError(code, message, violations, details);
        return ResponseEntity.status(status).body(responseFactory.failure(error, request));
    }

    private String localize(JetvamException exception) {
        MessageSource source = messageSourceProvider.getIfAvailable();
        if (source == null) {
            return exception.getMessage();
        }
        String messageKey = "error." + exception.code().toLowerCase().replace('.', '-');
        return source.getMessage(messageKey, null, exception.getMessage(), LocaleContextHolder.getLocale());
    }

    private FieldViolation fieldViolation(FieldError error) {
        String code = error.getCode() == null ? "VALIDATION.INVALID" : error.getCode();
        String message = error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage();
        return new FieldViolation(error.getField(), code, message);
    }
}
