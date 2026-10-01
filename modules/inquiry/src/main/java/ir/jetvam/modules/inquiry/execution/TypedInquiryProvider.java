package ir.jetvam.modules.inquiry.execution;

import ir.jetvam.common.inquiry.InquiryType;

/**
 * Owns request typing and provider-protocol decisions for one inquiry type.
 *
 * @param <C> canonical request type accepted by this inquiry
 * @author reza jamshidi
 * @since 10/1/2026
 */
public interface TypedInquiryProvider<C> {

    InquiryType inquiryType();

    Class<C> requestType();

    InquiryExecutionResult execute(String nationalCode, C request, InquiryExecutionContext context);
}
