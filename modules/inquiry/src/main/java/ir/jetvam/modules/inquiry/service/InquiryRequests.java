package ir.jetvam.modules.inquiry.service;

/**
 * Groups provider-neutral commands accepted by the Inquiry application boundary.
 * Provider adapters translate these canonical identifiers to their own wire contracts.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public final class InquiryRequests {

    private InquiryRequests() {
    }

    public record MobileOwnership(String mobile, String nationalCode) {
    }

    public record BadCheque(String nationalCode) {
    }

    public record CreditRating(String nationalCode) {
    }

    public record CivilRegistration(String nationalCode) {
    }

    public record MilitaryStatus(String nationalCode) {
    }

    public record BankAccountStatus(String nationalCode) {
    }

    public record BankingFacilities(String nationalCode) {
    }
}
