package ir.jetvam.modules.inquiry.service;

/**
 * Defines the provider-neutral boundary for applicant inquiries used across Jetvam modules.
 * Callers depend on normalized outcomes and never on Shahkar, bureau or bank wire models.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface InquiryService {

    InquiryResults.MobileOwnership verifyMobileOwnership(InquiryRequests.MobileOwnership request);

    InquiryResults.CivilRegistration findCivilRegistration(InquiryRequests.CivilRegistration request);

    InquiryResults.MilitaryStatus findMilitaryStatus(InquiryRequests.MilitaryStatus request);

    InquiryResults.BankAccountStatus findBankAccountStatus(InquiryRequests.BankAccountStatus request);

    InquiryResults.BankingFacilities findBankingFacilities(InquiryRequests.BankingFacilities request);

    InquiryResults.BadCheque findBadCheques(InquiryRequests.BadCheque request);

    InquiryResults.CreditRating findCreditRating(InquiryRequests.CreditRating request);
}
