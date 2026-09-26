package ir.jetvam.modules.inquiry.service;

/**
 * Delivers inquiry completion through one transport such as Spring Bean, MQ or webhook.
 * New delivery mechanisms register another implementation without changing Inquiry or its consumers.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface InquiryCallbackTransport {

    String type();

    void deliver(AsyncInquiryModels.Callback callback, AsyncInquiryModels.CompletionEvent event);
}
