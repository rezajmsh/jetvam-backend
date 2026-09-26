package ir.jetvam.modules.inquiry.service;

/**
 * Executes one asynchronous inquiry step without owning scheduling or application workflow state.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface DeferredInquiryService {

    DeferredInquiryModels.Result execute(DeferredInquiryModels.Command command);
}
