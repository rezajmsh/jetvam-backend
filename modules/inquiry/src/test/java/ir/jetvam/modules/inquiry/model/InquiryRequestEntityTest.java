package ir.jetvam.modules.inquiry.model;

import ir.jetvam.common.inquiry.InquiryType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the durable provider and callback state machines of an asynchronous inquiry.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
class InquiryRequestEntityTest {

    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");

    @Test
    void persistsProviderAffinityAndMakesCallbackReadyAfterCompletion() {
        InquiryRequestEntity request = request();

        request.start(NOW);
        request.waitForProvider(
                "credit-provider",
                "{\"stage\":\"POLL\",\"trackingCode\":\"tracking-42\"}",
                NOW.plusSeconds(60)
        );
        request.start(NOW.plusSeconds(60));
        request.complete(
                "credit-provider",
                "{\"stage\":\"POLL\",\"trackingCode\":\"tracking-42\"}",
                "{\"rank\":\"5\"}",
                NOW.plusSeconds(61)
        );

        assertThat(request.getStatus()).isEqualTo(InquiryStatus.COMPLETED);
        assertThat(request.getProviderCode()).isEqualTo("credit-provider");
        assertThat(request.getExecutionContextJson()).contains("\"stage\":\"POLL\"");
        assertThat(request.getExecutionContextJson()).contains("\"trackingCode\":\"tracking-42\"");
        assertThat(request.getAttemptCount()).isEqualTo(2);
        assertThat(request.getCallbackStatus()).isEqualTo(InquiryCallbackStatus.PENDING);
        assertThat(request.getCallbackNextAttemptAt()).isEqualTo(NOW.plusSeconds(61));
    }

    @Test
    void retriesCallbackWithoutRepeatingProviderExecution() {
        InquiryRequestEntity request = request();
        request.start(NOW);
        request.reject("cheque-provider", null, "UNSETTLED_CHEQUE", "Unsettled cheque exists", NOW);

        request.startCallback(NOW);
        request.retryCallback("consumer unavailable", NOW.plusSeconds(30));
        request.startCallback(NOW.plusSeconds(30));
        request.callbackDelivered();

        assertThat(request.getStatus()).isEqualTo(InquiryStatus.REJECTED);
        assertThat(request.getAttemptCount()).isEqualTo(1);
        assertThat(request.getCallbackAttemptCount()).isEqualTo(2);
        assertThat(request.getCallbackStatus()).isEqualTo(InquiryCallbackStatus.DELIVERED);
        assertThat(request.getCallbackError()).isNull();
    }

    private static InquiryRequestEntity request() {
        return new InquiryRequestEntity(
                InquiryType.CREDIT_RATING,
                "0013546789",
                "0013546789",
                "{\"nationalCode\":\"0013546789\"}",
                InquiryResponseMode.ASYNC_CALLBACK,
                "SPRING_BEAN",
                "origination-application-inquiry",
                "application-inquiry-id",
                NOW
        );
    }
}
