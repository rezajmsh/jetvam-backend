package ir.jetvam.modules.origination.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.service.CustomerIdentityFacts;
import ir.jetvam.modules.identity.service.CustomerIdentityQueryService;
import ir.jetvam.modules.identity.service.CustomerProfileDataService;
import ir.jetvam.modules.identity.service.CustomerProfileModels;
import ir.jetvam.modules.origination.model.ApplicationControlEntity;
import ir.jetvam.modules.origination.model.ApplicationControlStatus;
import ir.jetvam.modules.origination.model.ApplicationStatus;
import ir.jetvam.modules.origination.model.LoanApplicationEntity;
import ir.jetvam.modules.origination.repository.LoanApplicationRepository;
import ir.jetvam.modules.payment.model.FeeCategory;
import ir.jetvam.modules.payment.service.PaymentModels;
import ir.jetvam.modules.payment.service.PaymentService;
import ir.jetvam.modules.product.service.ProductCatalogService;
import ir.jetvam.modules.product.service.ProductViews;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Implements the customer application state machine from plan selection through credit allocation.
 * Plan rules and commercial values are snapshotted when the application is created.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultLoanApplicationService implements LoanApplicationService {

    private final LoanApplicationRepository applicationRepository;
    private final ProductCatalogService productCatalogService;
    private final CustomerIdentityQueryService identityQueryService;
    private final CustomerProfileDataService profileDataService;
    private final PaymentService paymentService;
    private final OriginationControlOrchestrator controlOrchestrator;
    private final TimeProvider timeProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public OriginationModels.ApplicationView create(
            UUID customerPartyId,
            OriginationModels.CreateApplication command
    ) {
        Preconditions.requireNonNull(command, "command");
        CustomerIdentityFacts customer = identityQueryService.getVerifiedCustomer(customerPartyId);
        ProductViews.Plan plan = productCatalogService.getActivePlan(command.planId());
        validateCommercialSelection(plan, command);
        CustomerProfileModels.ProfileView profile = profileDataService.get(customerPartyId);

        List<ProductViews.Control> controls = plan.controls().stream()
                .sorted(Comparator.comparingInt(ProductViews.Control::priority))
                .toList();
        boolean hasApplicationFee = plan.fees().stream()
                .anyMatch(fee -> fee.sourceInquiryCode() == null && fee.amount().signum() > 0);
        ApplicationStatus initialStatus = ApplicationStatus.WAITING_CONTROLS;
        LoanApplicationEntity application = new LoanApplicationEntity(
                customer.partyId(), customer.nationalCode(), customer.birthDate(), plan.id(), plan.code(), plan.name(),
                command.requestedAmount(), command.termMonths(), plan.annualInterestRate(),
                !plan.guarantees().isEmpty() || !plan.collaterals().isEmpty(),
                !plan.collaterals().isEmpty(), hasApplicationFee,
                personalRevision(profile), employmentRevision(profile), initialStatus
        );
        controls.forEach(control -> application.addControl(new ApplicationControlEntity(
                application, control.code(), control.title(), control.priority(), control.type(),
                control.minimumValue(), control.maximumValue(), control.sourceInquiryCode(), control.failureMessage()
        )));
        LoanApplicationEntity saved = applicationRepository.saveAndFlush(application);
        paymentService.createObligations(customer.partyId(), saved.getId(), plan.fees().stream()
                .map(DefaultLoanApplicationService::toFee)
                .toList());
        controlOrchestrator.advance(saved);
        return toView(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OriginationModels.ApplicationView> findMine(UUID customerPartyId) {
        return applicationRepository.findAllByCustomerPartyIdOrderByCreatedAtDesc(customerPartyId).stream()
                .map(this::toView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OriginationModels.ApplicationView getMine(UUID customerPartyId, UUID applicationId) {
        return toView(findOwned(customerPartyId, applicationId));
    }

    @Override
    @Transactional
    public OriginationModels.ApplicationView refresh(UUID customerPartyId, UUID applicationId) {
        LoanApplicationEntity application = findOwned(customerPartyId, applicationId);
        if (application.getStatus() == ApplicationStatus.WAITING_CONTROL_FEE
                && currentControlFeePaid(application)) {
            application.controlsPaid();
            controlOrchestrator.advance(application);
        }
        if (application.getStatus() == ApplicationStatus.WAITING_PERSONAL_INFORMATION
                || application.getStatus() == ApplicationStatus.WAITING_EMPLOYMENT_INFORMATION) {
            CustomerProfileModels.ProfileView profile = profileDataService.get(customerPartyId);
            application.adoptAvailableProfileInformation(
                    personalRevision(profile), employmentRevision(profile),
                    paymentService.allPaid(applicationId, FeeCategory.APPLICATION)
            );
        }
        if (application.getStatus() == ApplicationStatus.WAITING_APPLICATION_FEE
                && paymentService.allPaid(applicationId, FeeCategory.APPLICATION)) {
            application.applicationFeesPaid();
        }
        return toView(application);
    }

    @Override
    @Transactional
    public OriginationModels.ApplicationView savePersonalInformation(
            UUID customerPartyId,
            UUID applicationId,
            CustomerProfileModels.UpdatePersonalInformation command
    ) {
        LoanApplicationEntity application = findOwned(customerPartyId, applicationId);
        CustomerProfileModels.ProfileView profile = profileDataService.updatePersonalInformation(
                customerPartyId,
                command
        );
        application.usePersonalProfile(
                profile.personalInformation().revision(), employmentRevision(profile),
                paymentService.allPaid(applicationId, FeeCategory.APPLICATION)
        );
        return toView(application);
    }

    @Override
    @Transactional
    public OriginationModels.ApplicationView saveEmploymentInformation(
            UUID customerPartyId,
            UUID applicationId,
            CustomerProfileModels.UpdateEmploymentInformation command
    ) {
        LoanApplicationEntity application = findOwned(customerPartyId, applicationId);
        CustomerProfileModels.ProfileView profile = profileDataService.updateEmploymentInformation(
                customerPartyId,
                command
        );
        application.useEmploymentProfile(
                profile.employmentInformation().revision(),
                paymentService.allPaid(applicationId, FeeCategory.APPLICATION)
        );
        return toView(application);
    }

    @Override
    @Transactional
    public OriginationModels.ApplicationView saveGuaranteeInformation(
            UUID customerPartyId,
            UUID applicationId,
            OriginationModels.GuaranteeInformation command
    ) {
        LoanApplicationEntity application = findOwned(customerPartyId, applicationId);
        application.recordGuaranteeInformation(
                writeJson(command),
                paymentService.allPaid(applicationId, FeeCategory.APPLICATION)
        );
        return toView(application);
    }

    @Override
    @Transactional
    public OriginationModels.ApplicationView signContract(UUID customerPartyId, UUID applicationId) {
        LoanApplicationEntity application = findOwned(customerPartyId, applicationId);
        application.signContract(timeProvider.now());
        return toView(application);
    }

    @Override
    @Transactional
    public OriginationModels.ApplicationView markOriginalChequeReceived(UUID applicationId) {
        LoanApplicationEntity application = find(applicationId);
        application.originalChequeReceived();
        return toView(application);
    }

    @Override
    @Transactional
    public OriginationModels.ApplicationView allocateCredit(UUID applicationId) {
        LoanApplicationEntity application = find(applicationId);
        application.allocateCredit(timeProvider.now());
        return toView(application);
    }

    private LoanApplicationEntity findOwned(UUID customerPartyId, UUID applicationId) {
        return applicationRepository.findByIdAndCustomerPartyId(applicationId, customerPartyId)
                .orElseThrow(() -> new ResourceNotFoundException("loanApplication", applicationId));
    }

    private LoanApplicationEntity find(UUID applicationId) {
        return applicationRepository.findById(Preconditions.requireNonNull(applicationId, "applicationId"))
                .orElseThrow(() -> new ResourceNotFoundException("loanApplication", applicationId));
    }

    private OriginationModels.ApplicationView toView(LoanApplicationEntity application) {
        List<OriginationModels.ControlView> controls = application.getControls().stream()
                .sorted(Comparator.comparingInt(ApplicationControlEntity::getPriority))
                .map(control -> new OriginationModels.ControlView(
                        control.getId(), control.getControlCode(), control.getTitle(), control.getPriority(),
                        control.getControlType(), control.getSourceInquiryCode(), control.getStatus(),
                        control.getInquiryRequestId(), control.getObservedValue(), control.getErrorMessage()
                )).toList();
        return new OriginationModels.ApplicationView(
                application.getId(), application.getPlanId(), application.getPlanCode(), application.getPlanName(),
                application.getRequestedAmount(), application.getTermMonths(), application.getAnnualInterestRate(),
                application.getStatus(), application.getRejectionReason(), application.getPersonalProfileRevision(),
                application.getEmploymentProfileRevision(), controls,
                paymentService.findFees(application.getCustomerPartyId(), application.getId()),
                application.getCreatedAt(), application.getUpdatedAt()
        );
    }

    private static Long personalRevision(CustomerProfileModels.ProfileView profile) {
        return profile.personalInformation() == null ? null : profile.personalInformation().revision();
    }

    private static Long employmentRevision(CustomerProfileModels.ProfileView profile) {
        return profile.employmentInformation() == null ? null : profile.employmentInformation().revision();
    }

    private static PaymentModels.FeeDefinition toFee(ProductViews.Fee fee) {
        return new PaymentModels.FeeDefinition(
                fee.code(), fee.title(), fee.sourceInquiryCode() == null
                        ? FeeCategory.APPLICATION : FeeCategory.INQUIRY,
                fee.amount(), fee.currency(), fee.sourceInquiryCode() == null
                        ? PaymentModels.APPLICATION_APPROVED : fee.sourceInquiryCode()
        );
    }

    private boolean currentControlFeePaid(LoanApplicationEntity application) {
        return application.getControls().stream()
                .filter(control -> control.getStatus()
                        == ApplicationControlStatus.BLOCKED_BY_PAYMENT)
                .min(Comparator.comparingInt(ApplicationControlEntity::getPriority))
                .map(control -> paymentService.allPaid(application.getId(), control.getSourceInquiryCode()))
                .orElse(false);
    }

    private static void validateCommercialSelection(
            ProductViews.Plan plan,
            OriginationModels.CreateApplication command
    ) {
        BigDecimal amount = Preconditions.requireNonNull(command.requestedAmount(), "requestedAmount");
        Preconditions.require(amount.compareTo(plan.minimumAmount()) >= 0
                        && amount.compareTo(plan.maximumAmount()) <= 0,
                "requestedAmount is outside plan limits");
        Preconditions.require(command.termMonths() >= plan.minimumTermMonths()
                        && command.termMonths() <= plan.maximumTermMonths(),
                "termMonths is outside plan limits");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(Preconditions.requireNonNull(value, "value"));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to serialize application information", exception);
        }
    }
}
