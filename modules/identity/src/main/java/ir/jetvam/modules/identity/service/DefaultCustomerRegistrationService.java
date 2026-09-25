package ir.jetvam.modules.identity.service;

import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.common.security.UserCategory;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.IranianIdentifiers;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.IdentityOtpPurposes;
import ir.jetvam.modules.identity.model.AuthenticationMethod;
import ir.jetvam.modules.identity.persistence.CustomerProfileEntity;
import ir.jetvam.modules.identity.persistence.IndividualPartyEntity;
import ir.jetvam.modules.identity.persistence.UserAccountEntity;
import ir.jetvam.modules.identity.repository.CustomerProfileRepository;
import ir.jetvam.modules.identity.repository.IndividualPartyRepository;
import ir.jetvam.modules.identity.repository.UserAccountRepository;
import ir.jetvam.modules.integration.shahkar.ShahkarProvider;
import ir.jetvam.modules.integration.shahkar.ShahkarVerification;
import ir.jetvam.modules.otp.service.OtpChallengeService;
import ir.jetvam.modules.otp.service.OtpChallengeView;
import ir.jetvam.modules.otp.service.OtpVerificationData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final CustomerRegistrationTransactionService registrationTransactions;
    private final IndividualPartyRepository individualRepository;
    private final UserAccountRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final TimeProvider timeProvider;

    @Override
    public OtpChallengeView requestOtp(StartCustomerRegistrationCommand command) {
        Preconditions.requireNonNull(command, "command");
        String mobile = IranianIdentifiers.normalizeMobileNumber(command.mobile());
        String nationalCode = IranianIdentifiers.normalizeNationalCode(command.nationalCode());
        Preconditions.require(IranianIdentifiers.isValidMobileNumber(mobile), "mobile is invalid");
        Preconditions.require(IranianIdentifiers.isValidNationalCode(nationalCode), "nationalCode is invalid");
        return otpChallengeService.issue(mobile, nationalCode, IdentityOtpPurposes.CUSTOMER_REGISTRATION);
    }

    @Override
    public CustomerRegistrationResult verifyOtp(VerifyCustomerRegistrationCommand command) {
        Preconditions.requireNonNull(command, "command");
        OtpVerificationData verified = otpChallengeService.verify(
                command.challengeId(),
                command.otp(),
                IdentityOtpPurposes.CUSTOMER_REGISTRATION
        );
        registrationTransactions.assertRegistrationAvailable(verified.mobile(), verified.nationalCode());
        ShahkarVerification shahkar = shahkarProvider.verify(verified.mobile(), verified.nationalCode());
        CustomerRegistrationCompletion completion = registrationTransactions.complete(command, shahkar);
        if (!completion.matched()) {
            throw new ValidationException("Mobile ownership could not be verified");
        }
        return completion.registration();
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

}
