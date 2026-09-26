package ir.jetvam.modules.inquiry.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Delivers callbacks to keyed Spring beans in the current modular application.
 * Duplicate handler keys fail during startup instead of routing an outcome ambiguously.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Component
public class SpringBeanInquiryCallbackTransport implements InquiryCallbackTransport {

    private final Map<String, InquiryCompletionHandler> handlers;

    public SpringBeanInquiryCallbackTransport(List<InquiryCompletionHandler> handlers) {
        this.handlers = new HashMap<>();
        handlers.forEach(handler -> {
            InquiryCompletionHandler duplicate = this.handlers.putIfAbsent(handler.key(), handler);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate inquiry completion handler: " + handler.key());
            }
        });
    }

    @Override
    public String type() {
        return AsyncInquiryModels.SPRING_BEAN;
    }

    @Override
    public void deliver(AsyncInquiryModels.Callback callback, AsyncInquiryModels.CompletionEvent event) {
        InquiryCompletionHandler handler = handlers.get(callback.destination());
        if (handler == null) {
            throw new IllegalStateException("Unknown inquiry completion handler: " + callback.destination());
        }
        handler.handle(event);
    }
}
