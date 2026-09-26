package ir.jetvam.modules.inquiry.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.modules.inquiry.service.InquiryRequests;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes protected operational inquiry endpoints for trusted services and system staff.
 * Customer clients use owning workflows such as registration or origination instead of these endpoints.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jetvam.inquiry.api", name = "enabled", havingValue = "true")
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR', 'SERVICE') and hasAuthority('inquiry:execute')")
@Tag(name = "Inquiries", description = "Provider-neutral execution of protected external inquiries.")
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping("/mobile-ownership")
    @Operation(summary = "Verify mobile ownership", description = "Verifies that a mobile number belongs to a national code through Shahkar providers.")
    public InquiryResults.MobileOwnership mobileOwnership(@Valid @RequestBody MobileOwnershipRequest request) {
        return inquiryService.verifyMobileOwnership(new InquiryRequests.MobileOwnership(
                request.mobile(), request.nationalCode()
        ));
    }

    @PostMapping("/civil-registration")
    @Operation(summary = "Get civil-registration facts", description = "Returns normalized identity validity and life status for an applicant.")
    public InquiryResults.CivilRegistration civilRegistration(@Valid @RequestBody NationalCodeRequest request) {
        return inquiryService.findCivilRegistration(new InquiryRequests.CivilRegistration(request.nationalCode()));
    }

    @PostMapping("/military-status")
    @Operation(summary = "Get military status", description = "Returns normalized military status and current eligibility for an applicant.")
    public InquiryResults.MilitaryStatus militaryStatus(@Valid @RequestBody NationalCodeRequest request) {
        return inquiryService.findMilitaryStatus(new InquiryRequests.MilitaryStatus(request.nationalCode()));
    }

    @PostMapping("/bank-account-status")
    @Operation(summary = "Get bank-account status", description = "Returns normalized bank-account status and whether the account is active.")
    public InquiryResults.BankAccountStatus bankAccountStatus(@Valid @RequestBody NationalCodeRequest request) {
        return inquiryService.findBankAccountStatus(new InquiryRequests.BankAccountStatus(request.nationalCode()));
    }

    @PostMapping("/banking-facilities")
    @Operation(summary = "Get banking facilities", description = "Returns direct, indirect and overdue banking-facility facts for an applicant.")
    public InquiryResults.BankingFacilities bankingFacilities(@Valid @RequestBody NationalCodeRequest request) {
        return inquiryService.findBankingFacilities(new InquiryRequests.BankingFacilities(request.nationalCode()));
    }

    @PostMapping("/bad-cheques")
    @Operation(summary = "Get bad cheques", description = "Returns normalized unsettled-cheque count and total amount for an applicant.")
    public InquiryResults.BadCheque badCheques(@Valid @RequestBody NationalCodeRequest request) {
        return inquiryService.findBadCheques(new InquiryRequests.BadCheque(request.nationalCode()));
    }

    @PostMapping("/credit-rating")
    @Operation(summary = "Get credit rating", description = "Returns normalized credit code, rank and provider score for an applicant.")
    public InquiryResults.CreditRating creditRating(@Valid @RequestBody NationalCodeRequest request) {
        return inquiryService.findCreditRating(new InquiryRequests.CreditRating(request.nationalCode()));
    }

    public record MobileOwnershipRequest(@NotBlank String mobile, @NotBlank String nationalCode) {
    }

    public record NationalCodeRequest(@NotBlank String nationalCode) {
    }
}
