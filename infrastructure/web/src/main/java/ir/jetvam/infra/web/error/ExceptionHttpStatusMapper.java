package ir.jetvam.infra.web.error;

import org.springframework.http.HttpStatus;

public interface ExceptionHttpStatusMapper {

    HttpStatus statusFor(Throwable throwable);
}
