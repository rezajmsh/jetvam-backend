package ir.jetvam.infra.web.error;

import ir.jetvam.common.exception.ConflictException;
import ir.jetvam.common.exception.IntegrationException;
import ir.jetvam.common.exception.OperationNotAllowedException;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.common.exception.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the behavior of default exception http status mapper.
 * The tests protect the shared contract and its important edge cases.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

class DefaultExceptionHttpStatusMapperTest {

    private final DefaultExceptionHttpStatusMapper mapper = new DefaultExceptionHttpStatusMapper();

    @Test
    void mapsTheCommonExceptionHierarchyToStableHttpStatuses() {
        assertThat(mapper.statusFor(new ValidationException("invalid"))).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(mapper.statusFor(new ResourceNotFoundException("customer", 1))).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(mapper.statusFor(new ConflictException("duplicate"))).isEqualTo(HttpStatus.CONFLICT);
        assertThat(mapper.statusFor(new RateLimitExceededException("retry later", Instant.EPOCH)))
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(mapper.statusFor(new OperationNotAllowedException("approve", "forbidden")))
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(mapper.statusFor(new IntegrationException("core", "create-loan", new RuntimeException())))
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }
}
