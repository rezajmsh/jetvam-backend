package ir.jetvam.modules.inquiry.service;

/**
 * Exposes consumer-neutral provider and callback work to the jobs application.
 * Job handlers delegate here and contain no inquiry or consumer business logic.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface InquiryWorkerService {

    AsyncInquiryModels.BatchResult processBatch(int batchSize);
}
