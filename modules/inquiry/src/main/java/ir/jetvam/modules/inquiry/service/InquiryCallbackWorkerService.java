package ir.jetvam.modules.inquiry.service;

/**
 * Delivers callbacks for completed inquiries independently from provider execution.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
public interface InquiryCallbackWorkerService {

    AsyncInquiryModels.BatchResult processBatch(int batchSize);
}
