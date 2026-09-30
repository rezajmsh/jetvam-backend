package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.inquiry.service.DeferredInquiryStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the mock bureau models the real submit and poll interaction.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
class MockCreditRatingAdaptersTest {

    @Test
    void returnsTrackingCodeThenCompletesOnPoll() {
        MockCreditRatingSubmitAdapter submitAdapter = new MockCreditRatingSubmitAdapter();
        MockCreditRatingPollAdapter pollAdapter = new MockCreditRatingPollAdapter();

        CreditRatingProtocol.Progress submitted = submitAdapter.execute(
                new CreditRatingProtocol.Submit("0013546789"), null
        );
        CreditRatingProtocol.Progress completed = pollAdapter.execute(
                new CreditRatingProtocol.Poll(submitted.trackingCode()), null
        );

        assertThat(submitAdapter.capabilityCode()).isEqualTo(InquiryType.CREDIT_RATING_SUBMIT.code());
        assertThat(submitted.status()).isEqualTo(DeferredInquiryStatus.PENDING);
        assertThat(submitted.trackingCode()).startsWith("MOCK-CREDIT-");
        assertThat(completed.status()).isEqualTo(DeferredInquiryStatus.COMPLETED);
        assertThat(completed.trackingCode()).isEqualTo(submitted.trackingCode());
        assertThat(completed.rank()).isEqualTo(8);
    }
}
