package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.ConflictException;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.common.security.UserCategory;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.modules.identity.IdentityOtpPurposes;
import ir.jetvam.modules.identity.IdentityRoles;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.persistence.CustomerProfileEntity;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.persistence.RoleEntity;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.repository.CustomerProfileRepository;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import ir.jetvam.modules.identity.repository.RoleRepository;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import ir.jetvam.modules.inquiry.service.InquiryResults;
import ir.jetvam.modules.otp.service.OtpChallengeService;
import ir.jetvam.modules.otp.service.OtpVerificationData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/**
 * Owns the short database phases of customer registration around the Shahkar call.
 * OTP consumption and account provisioning commit atomically after external I/O has completed.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Service
@RequiredArgsConstructor
public class CustomerRegistrationTransactionService {

    private final OtpChallengeService otpChallengeService;
    private final IndividualPartyRepository individualRepository;
    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final TimeProvider timeProvider;

    @Transactional(readOnly = true)
    public void assertRegistrationAvailable(String mobile, String nationalCode) {
        IndividualPartyEntity individual = resolveExistingIndividual(mobile, nationalCode);
        if (individual != null && customerAccountExists(individual)) {
            throw new ConflictException("Customer account already exists", Map.of());
        }
    }

    @Transactional(noRollbackFor = ValidationException.class)
    public CustomerRegistrationCompletion complete(
            VerifyCustomerRegistrationCommand command,
            InquiryResults.MobileOwnership ownership
    ) {
        OtpVerificationData verified = otpChallengeService.consumeAtomically(
                command.challengeId(),
                command.otp(),
                IdentityOtpPurposes.CUSTOMER_REGISTRATION
        );
        IndividualPartyEntity individual = resolveOrCreateIndividual(verified.mobile(), verified.nationalCode());
        if (customerAccountExists(individual)) {
            throw new ConflictException("Customer account already exists", Map.of());
        }

        individual.markMobileVerified(timeProvider.now());
        individual.markShahkarPending();
        if (!ownership.matched()) {
            individual.markShahkarNotMatched(ownership.trackingId());
            return CustomerRegistrationCompletion.rejected();
        }
        individual.markShahkarMatched(timeProvider.now(), ownership.trackingId());
        individual.markIdentityVerified(timeProvider.now());

        CustomerProfileEntity profile = customerProfileRepository.findByPartyId(individual.getId())
                .orElseGet(() -> new CustomerProfileEntity(individual));
        profile.markMobileVerified();
        profile.markIdentityVerified();
        customerProfileRepository.save(profile);

        RoleEntity customerRole = roleRepository.findByCode(IdentityRoles.CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("role", IdentityRoles.CUSTOMER));
        UserAccountEntity account = userRepository.save(new UserAccountEntity(
                individual,
                null,
                null,
                AuthenticationMethod.OTP,
                Set.of(UserCategory.CUSTOMER),
                Set.of(customerRole)
        ));
        return CustomerRegistrationCompletion.accepted(new CustomerRegistrationResult(
                account.getId(),
                individual.getId(),
                profile.getOnboardingStatus()
        ));
    }

    private IndividualPartyEntity resolveOrCreateIndividual(String mobile, String nationalCode) {
        IndividualPartyEntity existing = resolveExistingIndividual(mobile, nationalCode);
        if (existing != null) {
            return existing;
        }
        return individualRepository.save(new IndividualPartyEntity(
                maskedCustomerName(mobile),
                nationalCode,
                null,
                null,
                null,
                mobile
        ));
    }

    private IndividualPartyEntity resolveExistingIndividual(String mobile, String nationalCode) {
        IndividualPartyEntity byNationalCode = individualRepository.findByNationalCode(nationalCode).orElse(null);
        IndividualPartyEntity byMobile = individualRepository.findByMobile(mobile).orElse(null);
        if (byNationalCode != null && byMobile != null && !byNationalCode.getId().equals(byMobile.getId())) {
            throw new ConflictException("Identity identifiers belong to different parties", Map.of());
        }
        IndividualPartyEntity existing = byNationalCode != null ? byNationalCode : byMobile;
        if (existing != null
                && (!nationalCode.equals(existing.getNationalCode()) || !mobile.equals(existing.getMobile()))) {
            throw new ConflictException("Customer identity does not match the existing party", Map.of());
        }
        return existing;
    }

    private boolean customerAccountExists(IndividualPartyEntity individual) {
        return userRepository.existsByParty_IdAndPrimaryAuthenticationMethod(
                individual.getId(),
                AuthenticationMethod.OTP
        );
    }

    private static String maskedCustomerName(String mobile) {
        return "Customer ******" + mobile.substring(mobile.length() - 4);
    }
}
