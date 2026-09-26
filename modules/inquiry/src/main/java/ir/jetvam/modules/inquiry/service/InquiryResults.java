package ir.jetvam.modules.inquiry.service;

import java.math.BigDecimal;

/**
 * Groups normalized inquiry outcomes independently from any provider response schema.
 * Credit rank uses a higher-is-better integer so plan policies remain provider-neutral.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class InquiryResults {

    private InquiryResults() {
    }

    public record MobileOwnership(boolean matched, String trackingId) {
    }

    public record BadCheque(int unsettledCount, BigDecimal totalAmount, String trackingId) {

        public boolean hasUnsettledCheques() {
            return unsettledCount > 0;
        }
    }

    public record CreditRating(String ratingCode, int rank, BigDecimal score, String trackingId) {
    }

    public record CivilRegistration(boolean identityValid, boolean alive, String trackingId) {
    }

    public record MilitaryStatus(String statusCode, boolean eligible, String trackingId) {
    }

    public record BankAccountStatus(String statusCode, boolean active, String trackingId) {
    }

    public record BankingFacilities(
            int directFacilityCount,
            int indirectFacilityCount,
            boolean hasOverdueDebt,
            BigDecimal overdueAmount,
            String trackingId
    ) {
    }
}
