package ir.jetvam.modules.inquiry.service;

/**
 * Handles terminal inquiry callbacks addressed to an in-process Spring consumer.
 * Implementations must be idempotent because callback delivery is at least once.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface InquiryCompletionHandler {

    String key();

    void handle(AsyncInquiryModels.CompletionEvent event);
}
