package ir.jetvam.modules.inquiry.provider;

import ir.jetvam.modules.inquiry.InquiryCapabilities;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the local Shahkar adapter without bypassing the provider contract.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
class MockMobileOwnershipAdapterTest {

    @Test
    void returnsSuccessfulOwnershipResult() {
        MockMobileOwnershipAdapter adapter = new MockMobileOwnershipAdapter();

        var result = adapter.execute(
                new InquiryRequests.MobileOwnership("09121234567", "0013546789"),
                null
        );

        assertThat(adapter.capabilityCode()).isEqualTo(InquiryCapabilities.MOBILE_OWNERSHIP);
        assertThat(adapter.adapterCode()).isEqualTo(MockMobileOwnershipAdapter.ADAPTER_CODE);
        assertThat(result.matched()).isTrue();
        assertThat(result.trackingId()).startsWith("MOCK-SHAHKAR-");
    }
}
