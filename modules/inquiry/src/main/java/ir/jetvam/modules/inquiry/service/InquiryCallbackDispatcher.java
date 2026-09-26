package ir.jetvam.modules.inquiry.service;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes a persisted callback to the transport selected by the consumer.
 * The registry is transport-neutral and supports future MQ or webhook implementations.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Component
public class InquiryCallbackDispatcher {

    private final Map<String, InquiryCallbackTransport> transports;

    public InquiryCallbackDispatcher(List<InquiryCallbackTransport> transports) {
        this.transports = new HashMap<>();
        transports.forEach(transport -> {
            InquiryCallbackTransport duplicate = this.transports.putIfAbsent(transport.type(), transport);
            if (duplicate != null) {
                throw new IllegalStateException("Duplicate inquiry callback transport: " + transport.type());
            }
        });
    }

    public void dispatch(AsyncInquiryModels.CallbackWork work) {
        InquiryCallbackTransport transport = transports.get(work.callback().transport());
        if (transport == null) {
            throw new IllegalStateException("Unknown inquiry callback transport: " + work.callback().transport());
        }
        transport.deliver(work.callback(), work.event());
    }
}
