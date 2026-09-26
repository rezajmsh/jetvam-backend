package ir.jetvam.modules.inquiry.service;

import java.util.UUID;

/**
 * Accepts idempotent asynchronous inquiry requests from any business consumer.
 * Consumers identify their callback without exposing provider or worker details.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface AsyncInquiryService {

    UUID submit(AsyncInquiryModels.Submit command);
}
