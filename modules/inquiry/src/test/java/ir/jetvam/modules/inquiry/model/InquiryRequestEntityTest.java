package ir.jetvam.modules.inquiry.model;

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
        request.waitForProvider("credit-provider", "tracking-42", NOW.plusSeconds(60));
        request.start(NOW.plusSeconds(60));
        request.complete("credit-provider", "tracking-42", "{\"rank\":\"5\"}", NOW.plusSeconds(61));

        assertThat(request.getStatus()).isEqualTo(AsyncInquiryStatus.COMPLETED);
        assertThat(request.getProviderCode()).isEqualTo("credit-provider");
        assertThat(request.getExternalTrackingCode()).isEqualTo("tracking-42");
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

        assertThat(request.getStatus()).isEqualTo(AsyncInquiryStatus.REJECTED);
        assertThat(request.getAttemptCount()).isEqualTo(1);
        assertThat(request.getCallbackAttemptCount()).isEqualTo(2);
        assertThat(request.getCallbackStatus()).isEqualTo(InquiryCallbackStatus.DELIVERED);
        assertThat(request.getCallbackError()).isNull();
    }

    private static InquiryRequestEntity request() {
        return new InquiryRequestEntity(
                "CREDIT_RATING_INQUIRY",
                "0013546789",
                "SPRING_BEAN",
                "origination-application-inquiry",
                "application-inquiry-id",
                NOW
        );
    }
}
