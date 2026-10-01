package ir.jetvam.modules.inquiry.service;

/**
 * Claims and executes provider-side inquiry work without delivering consumer callbacks.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
public interface InquiryExecutionWorkerService {

    AsyncInquiryModels.BatchResult processBatch(int batchSize);
}
