package ir.jetvam.modules.inquiry;

import ir.jetvam.common.inquiry.InquiryType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the stable mapping between persisted inquiry codes and executable types.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
class InquiryTypeTest {

    @Test
    void resolvesPersistedCodesCaseInsensitively() {
        assertThat(InquiryType.fromCode("bad_cheque_inquiry"))
                .isEqualTo(InquiryType.BAD_CHEQUE);
    }
}
