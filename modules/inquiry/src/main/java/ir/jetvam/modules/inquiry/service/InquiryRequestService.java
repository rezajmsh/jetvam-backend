package ir.jetvam.modules.inquiry.service;

/**
 * Single submission boundary for every inquiry response mode.
 *
 * @author reza jamshidi
 * @since 9/30/2026
 */
public interface InquiryRequestService {

    InquirySubmissionModels.Result submit(InquirySubmissionModels.Command command);
}
