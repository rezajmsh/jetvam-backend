package ir.jetvam.modules.inquiry.service;

/**
 * Executes one already-claimed persisted inquiry request.
 * Both inline synchronous coordination and the background worker call this same boundary.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
public interface InquiryRequestExecutor {

    AsyncInquiryModels.WorkResult execute(AsyncInquiryModels.WorkItem work);
}
