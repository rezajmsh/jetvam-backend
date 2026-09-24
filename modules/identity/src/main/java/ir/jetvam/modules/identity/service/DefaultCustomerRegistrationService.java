package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.ConflictException;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.common.security.UserCategory;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.IdentityRoles;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.model.OtpPurpose;
import ir.jetvam.modules.identity.persistence.CustomerProfileEntity;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.persistence.RoleEntity;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.repository.CustomerProfileRepository;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import ir.jetvam.modules.identity.repository.RoleRepository;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import ir.jetvam.modules.integration.shahkar.ShahkarProvider;
import ir.jetvam.modules.integration.shahkar.ShahkarVerification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates OTP, Shahkar and initial customer account provisioning.
 * Existing parties are reused only when both canonical identifiers agree.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultCustomerRegistrationService implements CustomerRegistrationService {

    private final OtpChallengeService otpChallengeService;
    private final ShahkarProvider shahkarProvider;
    private final IndividualPartyRepository individualRepository;
    private final UserAccountRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final TimeProvider timeProvider;

    @Override
    public OtpChallengeView requestOtp(StartCustomerRegistrationCommand command) {
        Preconditions.requireNonNull(command, "command");
        String mobile = IranianIdentifiers.normalizeMobileNumber(command.mobile());
        String nationalCode = IranianIdentifiers.normalizeNationalCode(command.nationalCode());
        Preconditions.require(IranianIdentifiers.isValidMobileNumber(mobile), "mobile is invalid");
        Preconditions.require(IranianIdentifiers.isValidNationalCode(nationalCode), "nationalCode is invalid");
        return otpChallengeService.issue(mobile, nationalCode, OtpPurpose.CUSTOMER_REGISTRATION);
    }

    @Override
    @Transactional
    public CustomerRegistrationResult verifyOtp(VerifyCustomerRegistrationCommand command) {
        Preconditions.requireNonNull(command, "command");
        OtpVerificationData verified = otpChallengeService.consume(
                command.challengeId(),
                command.otp(),
                OtpPurpose.CUSTOMER_REGISTRATION
        );
        IndividualPartyEntity individual = resolveOrCreateIndividual(verified.mobile(), verified.nationalCode());
        if (userRepository.existsByParty_IdAndPrimaryAuthenticationMethod(
                individual.getId(),
                AuthenticationMethod.OTP
        )) {
            throw new ConflictException("Customer account already exists", Map.of());
        }

        individual.markMobileVerified(timeProvider.now());
        individual.markShahkarPending();
        ShahkarVerification shahkar = shahkarProvider.verify(verified.mobile(), verified.nationalCode());
        if (!shahkar.matched()) {
            individual.markShahkarNotMatched(shahkar.trackingId());
            throw new ValidationException("Mobile ownership could not be verified");
        }
        individual.markShahkarMatched(timeProvider.now(), shahkar.trackingId());
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
        return new CustomerRegistrationResult(
                account.getId(),
                individual.getId(),
                profile.getOnboardingStatus()
        );
    }

    @Override
    @Transactional
    public CustomerProfileView completeProfile(UUID userId, CompleteCustomerProfileCommand command) {
        Preconditions.requireNonNull(userId, "userId");
        Preconditions.requireNonNull(command, "command");
        String firstName = Preconditions.requireText(command.firstName(), "firstName").strip();
        String lastName = Preconditions.requireText(command.lastName(), "lastName").strip();
        LocalDate birthDate = Preconditions.requireNonNull(command.birthDate(), "birthDate");
        Preconditions.require(birthDate.isBefore(timeProvider.today()), "birthDate must be in the past");

        UserAccountEntity account = userRepository.findById(userId)
                .filter(candidate -> candidate.getPrimaryAuthenticationMethod() == AuthenticationMethod.OTP)
                .filter(candidate -> candidate.getCategories().contains(UserCategory.CUSTOMER))
                .orElseThrow(() -> new ResourceNotFoundException("customerUser", userId));
        IndividualPartyEntity individual = individualRepository.findById(account.getParty().getId())
                .orElseThrow(() -> new ResourceNotFoundException("individualParty", account.getParty().getId()));
        CustomerProfileEntity profile = customerProfileRepository.findByPartyId(account.getParty().getId())
                .orElseThrow(() -> new ResourceNotFoundException("customerProfile", account.getParty().getId()));

        individual.completeIdentity(firstName, lastName, birthDate);
        account.getParty().changeDisplayName(firstName + " " + lastName);
        profile.complete();
        return new CustomerProfileView(
                account.getId(),
                account.getParty().getId(),
                individual.getFirstName(),
                individual.getLastName(),
                individual.getBirthDate(),
                profile.getOnboardingStatus()
        );
    }

    private IndividualPartyEntity resolveOrCreateIndividual(String mobile, String nationalCode) {
        IndividualPartyEntity byNationalCode = individualRepository.findByNationalCode(nationalCode).orElse(null);
        IndividualPartyEntity byMobile = individualRepository.findByMobile(mobile).orElse(null);
        if (byNationalCode != null && byMobile != null && !byNationalCode.getId().equals(byMobile.getId())) {
            throw new ConflictException("Identity identifiers belong to different parties", Map.of());
        }
        IndividualPartyEntity existing = byNationalCode != null ? byNationalCode : byMobile;
        if (existing != null) {
            if (!nationalCode.equals(existing.getNationalCode()) || !mobile.equals(existing.getMobile())) {
                throw new ConflictException("Customer identity does not match the existing party", Map.of());
            }
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

    private static String maskedCustomerName(String mobile) {
        return "Customer ******" + mobile.substring(mobile.length() - 4);
    }
}
