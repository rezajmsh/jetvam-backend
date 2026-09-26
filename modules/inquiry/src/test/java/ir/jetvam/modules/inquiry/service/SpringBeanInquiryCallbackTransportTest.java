package ir.jetvam.modules.inquiry.service;

import ir.jetvam.modules.inquiry.model.AsyncInquiryStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies keyed in-process callback routing and its fail-fast configuration checks.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
class SpringBeanInquiryCallbackTransportTest {

    @Test
    void deliversOnlyToTheConfiguredConsumerHandler() {
        AtomicReference<AsyncInquiryModels.CompletionEvent> received = new AtomicReference<>();
        InquiryCompletionHandler handler = handler("origination", received);
        SpringBeanInquiryCallbackTransport transport = new SpringBeanInquiryCallbackTransport(List.of(handler));
        AsyncInquiryModels.CompletionEvent event = new AsyncInquiryModels.CompletionEvent(
                java.util.UUID.randomUUID(), "CREDIT_RATING_INQUIRY", AsyncInquiryStatus.COMPLETED,
                Map.of("rank", "5"), null, null, "local-id"
        );

        transport.deliver(new AsyncInquiryModels.Callback("SPRING_BEAN", "origination", "local-id"), event);

        assertThat(received.get()).isSameAs(event);
    }

    @Test
    void rejectsDuplicateHandlerDestinationsAtStartup() {
        AtomicReference<AsyncInquiryModels.CompletionEvent> received = new AtomicReference<>();

        assertThatThrownBy(() -> new SpringBeanInquiryCallbackTransport(List.of(
                handler("origination", received), handler("origination", received)
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate inquiry completion handler");
    }

    private static InquiryCompletionHandler handler(
            String key,
            AtomicReference<AsyncInquiryModels.CompletionEvent> received
    ) {
        return new InquiryCompletionHandler() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public void handle(AsyncInquiryModels.CompletionEvent event) {
                received.set(event);
            }
        };
    }
}
