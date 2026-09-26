package ir.jetvam.modules.payment.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies fee activation, payment and idempotent terminal transitions.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
class FeeObligationEntityTest {

    @Test
    void successfulPaymentCompletesTheFeeIdempotently() {
        FeeObligationEntity fee = new FeeObligationEntity(
                UUID.randomUUID(), "loan_application", UUID.randomUUID(), "inquiry_fee", "Inquiry fee",
                FeeCategory.INQUIRY, BigDecimal.valueOf(800_000), "irr", null
        );
        Instant paidAt = Instant.parse("2026-09-25T08:00:00Z");

        fee.markPaid(paidAt);
        fee.markPaid(paidAt.plusSeconds(10));

        assertThat(fee.getStatus()).isEqualTo(FeeStatus.PAID);
        assertThat(fee.getPaidAt()).isEqualTo(paidAt);
    }

    @Test
    void inquiryFeeCannotBePaidBeforeItsControlIsActivated() {
        FeeObligationEntity fee = new FeeObligationEntity(
                UUID.randomUUID(), "loan_application", UUID.randomUUID(), "credit_fee", "Credit fee",
                FeeCategory.INQUIRY, BigDecimal.valueOf(800_000), "irr", "CREDIT_RATING_INQUIRY"
        );

        assertThat(fee.getStatus()).isEqualTo(FeeStatus.BLOCKED);

        fee.activate("CREDIT_RATING_INQUIRY");

        assertThat(fee.getStatus()).isEqualTo(FeeStatus.PENDING);
    }
}
