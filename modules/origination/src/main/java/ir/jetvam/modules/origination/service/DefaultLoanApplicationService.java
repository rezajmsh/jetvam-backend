package ir.jetvam.modules.origination.service;

import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.identity.service.CustomerIdentityFacts;
import ir.jetvam.modules.identity.service.CustomerIdentityQueryService;
import ir.jetvam.modules.identity.service.CustomerProfileDataService;
import ir.jetvam.modules.identity.service.CustomerProfileModels;
import ir.jetvam.modules.inquiry.service.InquiryDefinitionService;
import ir.jetvam.modules.origination.model.ApplicationEmploymentEntity;
import ir.jetvam.modules.origination.model.ApplicationChequeCollateralEntity;
import ir.jetvam.modules.origination.model.ApplicationCollateralEntity;
import ir.jetvam.modules.origination.model.ApplicationCollateralDocumentRequirementEntity;
import ir.jetvam.modules.origination.model.ApplicationDocumentEntity;
import ir.jetvam.modules.origination.model.ApplicationDocumentStatus;
import ir.jetvam.modules.origination.model.ApplicationCollateralStatus;
import ir.jetvam.modules.origination.model.ApplicationControlEntity;
import ir.jetvam.modules.origination.model.ApplicationControlStatus;
import ir.jetvam.modules.origination.model.ApplicationGuarantorEntity;
import ir.jetvam.modules.origination.model.CollateralProviderType;
import ir.jetvam.modules.origination.model.ApplicationStatus;
import ir.jetvam.modules.origination.model.LoanApplicationEntity;
import ir.jetvam.modules.origination.repository.LoanApplicationRepository;
import ir.jetvam.modules.origination.repository.ApplicationEmploymentRepository;
import ir.jetvam.modules.origination.repository.ApplicationChequeCollateralRepository;
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
import java.util.Map;

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
    private final InquiryDefinitionService inquiryDefinitionService;
    private final ApplicationEmploymentRepository employmentRepository;
    private final PaymentService paymentService;
    private final OriginationControlOrchestrator controlOrchestrator;
    private final TimeProvider timeProvider;
    private final ApplicationChequeCollateralRepository chequeCollateralRepository;
    private final ApplicationDocumentStorage documentStorage;

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
        List<ProductViews.Control> controls = plan.controls().stream()
                .filter(ProductViews.Control::enabled)
                .sorted(Comparator.comparingInt(ProductViews.Control::priority))
                .toList();
        boolean hasApplicationFee = plan.fees().stream()
                .anyMatch(fee -> fee.sourceInquiryCode() == null && fee.amount().signum() > 0);
        validateInquiryDependencies(controls);
        ApplicationStatus initialStatus = ApplicationStatus.WAITING_CONTROL_CONFIRMATION;
        LoanApplicationEntity application = new LoanApplicationEntity(
                customer.partyId(), customer.nationalCode(), customer.birthDate(), plan.id(), plan.code(), plan.name(),
                command.requestedAmount(), command.termMonths(), plan.annualInterestRate(),
                hasApplicationFee, initialStatus
        );
        controls.forEach(control -> application.addControl(new ApplicationControlEntity(
                application, control.subjectType(),
                control.subjectType() == ir.jetvam.modules.product.model.ControlSubjectType.APPLICANT
                        ? customer.partyId() : null,
                control.code(), control.title(), control.priority(), control.type(),
                control.minimumValue(), control.maximumValue(),
                control.sourceInquiryCode() == null ? null : InquiryType.fromCode(control.sourceInquiryCode()),
                control.failureMessage()
        )));
        snapshotGuarantorRequirements(application, plan);
        LoanApplicationEntity saved = applicationRepository.saveAndFlush(application);
        snapshotCollateralRequirements(saved, customer.partyId(), plan);
        saved = applicationRepository.saveAndFlush(saved);
        List<PaymentModels.FeeDefinition> configuredFees = plan.fees().stream()
                .filter(ProductViews.Fee::enabled)
                .map(DefaultLoanApplicationService::toFee)
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        paymentService.createObligations(customer.partyId(), saved.getId(), configuredFees);
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
    public List<OriginationModels.ApplicationView> findAllForOperations() {
        return applicationRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toView).toList();
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
        if (application.getStatus() == ApplicationStatus.WAITING_APPLICATION_FEE
                && paymentService.allPaid(applicationId, FeeCategory.APPLICATION)) {
            application.applicationFeesPaid();
        }
        return toView(application);
    }

    @Override
    @Transactional
    public OriginationModels.ApplicationView confirmControls(UUID customerPartyId, UUID applicationId) {
        LoanApplicationEntity application = findOwned(customerPartyId, applicationId);
        application.confirmControls();
        controlOrchestrator.advance(application);
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
        Preconditions.require(profile.personalInformationComplete(), "personal information is incomplete");
        return toView(application);
    }

    @Override
    @Transactional
    public OriginationModels.ApplicationView saveEmploymentInformation(
            UUID customerPartyId,
            UUID applicationId,
            OriginationModels.EmploymentInformation command
    ) {
        LoanApplicationEntity application = findOwned(customerPartyId, applicationId);
        ApplicationEmploymentEntity employment = employmentRepository.findById(applicationId)
                .orElseGet(() -> new ApplicationEmploymentEntity(
                        application, command.educationCode(), command.employmentCode(), command.monthlyIncome()
                ));
        employment.update(command.educationCode(), command.employmentCode(), command.monthlyIncome());
        employmentRepository.save(employment);
        application.employmentInformationSaved(paymentService.allPaid(applicationId, FeeCategory.APPLICATION));
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
        ApplicationGuarantorEntity guarantor = application.getGuarantors().stream()
                .filter(item -> item.getNationalCode() == null)
                .findFirst().orElse(null);
        if (guarantor != null && command.guarantorNationalCode() != null && command.guarantorMobile() != null) {
            UUID guarantorPartyId = identityQueryService.findIndividualPartyId(
                    command.guarantorNationalCode(), command.guarantorMobile()
            ).orElse(null);
            guarantor.identify(command.guarantorNationalCode(), command.guarantorMobile(), guarantorPartyId);
        }
        application.getCollaterals().stream()
                .filter(item -> item.getProviderType() == CollateralProviderType.GUARANTOR)
                .filter(item -> item.getProviderPartyId() == null)
                .filter(item -> guarantor == null || item.getGuarantor().getId().equals(guarantor.getId()))
                .forEach(item -> {
                    if (guarantor != null && guarantor.getPartyId() != null) {
                        item.assignProvider(guarantor.getPartyId());
                    }
                });
        ApplicationCollateralEntity chequeCollateral = application.getCollaterals().stream()
                .filter(item -> "SAYAD_CHEQUE".equals(item.getHandlerCode()))
                .filter(item -> item.getStatus() == ApplicationCollateralStatus.WAITING_INFORMATION)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No cheque collateral is waiting for information"));
        ApplicationChequeCollateralEntity cheque = chequeCollateralRepository.findById(chequeCollateral.getId())
                .orElseGet(() -> new ApplicationChequeCollateralEntity(
                        chequeCollateral, command.chequeSayadId(), command.bankCode(), command.chequeNumber(),
                        command.chequeSerial(), command.chequeDate(), command.chequeAmount()
                ));
        cheque.update(command.chequeSayadId(), command.bankCode(), command.chequeNumber(),
                command.chequeSerial(), command.chequeDate(), command.chequeAmount());
        chequeCollateralRepository.save(cheque);
        chequeCollateral.informationCompleted(timeProvider.now());
        completeGuaranteeWhenReady(application);
        return toView(application);
    }

    @Override
    @Transactional
    public OriginationModels.ApplicationView uploadCollateralDocument(
            UUID customerPartyId,
            UUID applicationId,
            UUID collateralId,
            UUID requirementId,
            OriginationModels.UploadDocument command
    ) {
        Preconditions.requireNonNull(command, "command");
        LoanApplicationEntity application = findOwned(customerPartyId, applicationId);
        Preconditions.require(application.getStatus() == ApplicationStatus.WAITING_GUARANTEE,
                "Application is not waiting for guarantee information");
        ApplicationCollateralEntity collateral = application.getCollaterals().stream()
                .filter(item -> item.getId().equals(collateralId))
                .findFirst().orElseThrow(() -> new ResourceNotFoundException("applicationCollateral", collateralId));
        ApplicationCollateralDocumentRequirementEntity requirement = collateral.getDocumentRequirements().stream()
                .filter(item -> item.getId().equals(requirementId))
                .findFirst().orElseThrow(() -> new ResourceNotFoundException(
                        "collateralDocumentRequirement", requirementId));
        String contentType = Preconditions.requireText(command.contentType(), "contentType")
                .strip().toLowerCase();
        byte[] content = Preconditions.requireNonNull(command.content(), "content");
        Preconditions.require(content.length > 0, "Document content must not be empty");
        Preconditions.require(content.length <= requirement.getMaximumSizeBytes(),
                "Document exceeds the configured maximum size");
        Preconditions.require(requirement.accepts(contentType), "Document content type is not allowed");
        ApplicationDocumentStorage.StoredDocument stored = documentStorage.store(
                applicationId, command.originalFilename(), content
        );
        try {
            requirement.addDocument(new ApplicationDocumentEntity(
                    application, requirement, stored.storageKey(), command.originalFilename(), contentType,
                    stored.sizeBytes(), stored.checksumSha256(), customerPartyId
            ));
            collateral.documentsUpdated();
            completeGuaranteeWhenReady(application);
            applicationRepository.saveAndFlush(application);
            return toView(application);
        } catch (RuntimeException exception) {
            documentStorage.delete(stored.storageKey());
            throw exception;
        }
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
    public OriginationModels.ApplicationView markOriginalCollateralsReceived(UUID applicationId) {
        LoanApplicationEntity application = find(applicationId);
        application.getCollaterals().stream()
                .filter(item -> item.getStatus() == ApplicationCollateralStatus.WAITING_ORIGINAL_DELIVERY)
                .forEach(item -> item.originalReceived(timeProvider.now()));
        application.originalCollateralsReceived();
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
        OriginationModels.EmploymentInformation employment = employmentRepository.findById(application.getId())
                .map(item -> new OriginationModels.EmploymentInformation(
                        item.getEducationCode(), item.getEmploymentCode(), item.getMonthlyIncome(), List.of()
                )).orElse(null);
        List<OriginationModels.ControlView> controls = application.getControls().stream()
                .sorted(Comparator.comparing(ApplicationControlEntity::getSubjectType)
                        .thenComparingInt(ApplicationControlEntity::getPriority))
                .map(control -> new OriginationModels.ControlView(
                        control.getId(), control.getControlCode(), control.getTitle(), control.getPriority(),
                        control.getControlType(), control.getSubjectType(), control.getSubjectPartyId(),
                        control.getSourceInquiryCode() == null ? null : control.getSourceInquiryCode().code(),
                        control.getStatus(),
                        control.getInquiryRequestId(), control.getObservedValue(), control.getErrorMessage()
                )).toList();
        List<OriginationModels.GuarantorView> guarantors = application.getGuarantors().stream()
                .sorted(Comparator.comparingInt(ApplicationGuarantorEntity::getSequenceNumber))
                .map(item -> new OriginationModels.GuarantorView(
                        item.getId(), item.getSequenceNumber(), item.isRequired(), item.getPartyId(),
                        item.getNationalCode(), item.getMobile(), item.getStatus()
                )).toList();
        List<OriginationModels.CollateralView> collaterals = application.getCollaterals().stream()
                .sorted(Comparator.comparing(ApplicationCollateralEntity::getProviderType)
                        .thenComparing(ApplicationCollateralEntity::getCollateralTypeTitle))
                .map(item -> new OriginationModels.CollateralView(
                        item.getId(), item.getProviderType(), item.getProviderPartyId(),
                        item.getGuarantor() == null ? null : item.getGuarantor().getId(), item.getCollateralTypeId(),
                        item.getCollateralTypeCode(), item.getCollateralTypeTitle(), item.getHandlerCode(),
                        item.getMinimumCoveragePercent(), item.isRequired(), item.isRequiresPhysicalDelivery(),
                        item.getStatus(), item.getDocumentRequirements().stream()
                                .sorted(Comparator.comparingInt(
                                        ApplicationCollateralDocumentRequirementEntity::getDisplayOrder))
                                .map(requirement -> new OriginationModels.CollateralDocumentRequirementView(
                                        requirement.getId(), requirement.getDocumentTypeId(),
                                        requirement.getDocumentTypeCode(), requirement.getDocumentTypeTitle(),
                                        requirement.getAllowedContentTypes(), requirement.getMaximumSizeBytes(),
                                        requirement.isRequired(), requirement.getMinimumCount(),
                                        requirement.getMaximumCount(), (int) requirement.activeDocumentCount(),
                                        requirement.isSatisfied(), requirement.getDocuments().stream()
                                                .filter(document -> document.getStatus() == ApplicationDocumentStatus.ACTIVE)
                                                .sorted(Comparator.comparing(ApplicationDocumentEntity::getCreatedAt))
                                                .map(document -> new OriginationModels.ApplicationDocumentView(
                                                        document.getId(), document.getOriginalFilename(),
                                                        document.getContentType(), document.getSizeBytes(),
                                                        document.getChecksumSha256(), document.getCreatedAt()
                                                )).toList()
                                )).toList()
                )).toList();
        return new OriginationModels.ApplicationView(
                application.getId(), application.getPlanId(), application.getPlanCode(), application.getPlanName(),
                application.getRequestedAmount(), application.getTermMonths(), application.getAnnualInterestRate(),
                application.getStatus(), application.getRejectionReason(), employment, controls, guarantors, collaterals,
                paymentService.findFees(application.getCustomerPartyId(), application.getId()),
                ApplicationJourneyProjector.stages(application.getStatus()),
                ApplicationJourneyProjector.currentAction(application.getStatus()),
                application.getCreatedAt(), application.getUpdatedAt()
        );
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
                .map(control -> paymentService.allPaid(application.getId(), control.getSourceInquiryCode().code()))
                .orElse(false);
    }

    private void validateInquiryDependencies(List<ProductViews.Control> controls) {
        Map<String, Boolean> enabledDefinitions = inquiryDefinitionService.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(
                        InquiryDefinitionService.DefinitionView::inquiryCode,
                        InquiryDefinitionService.DefinitionView::enabled
                ));
        controls.stream().map(ProductViews.Control::sourceInquiryCode)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .forEach(code -> Preconditions.require(Boolean.TRUE.equals(enabledDefinitions.get(code)),
                        "Unknown or disabled inquiry dependency: " + code));
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

    private static void snapshotGuarantorRequirements(LoanApplicationEntity application, ProductViews.Plan plan) {
        ProductViews.GuarantorPolicy policy = plan.guarantorPolicy();
        if (policy == null || !policy.enabled()) {
            return;
        }
        for (int sequence = 1; sequence <= policy.minimumCount(); sequence++) {
            ApplicationGuarantorEntity guarantor = new ApplicationGuarantorEntity(
                    application, sequence, policy.required()
            );
            application.addGuarantor(guarantor);
        }
    }

    private static void snapshotCollateralRequirements(
            LoanApplicationEntity application, UUID applicantPartyId, ProductViews.Plan plan
    ) {
        plan.collaterals().stream().filter(ProductViews.Collateral::enabled).forEach(rule ->
                application.addCollateral(collateral(
                        application, CollateralProviderType.APPLICANT, applicantPartyId, null, rule
                )));
        for (ApplicationGuarantorEntity guarantor : application.getGuarantors()) {
            plan.guarantorCollaterals().stream().filter(ProductViews.Collateral::enabled).forEach(rule ->
                    application.addCollateral(collateral(
                            application, CollateralProviderType.GUARANTOR, null, guarantor, rule
                    )));
        }
    }

    private static ApplicationCollateralEntity collateral(
            LoanApplicationEntity application, CollateralProviderType providerType, UUID providerPartyId,
            ApplicationGuarantorEntity guarantor, ProductViews.Collateral rule
    ) {
        ApplicationCollateralEntity collateral = new ApplicationCollateralEntity(
                application, providerType, providerPartyId, guarantor, rule.collateralTypeId(), rule.code(),
                rule.title(), rule.handlerCode(), rule.minimumCoveragePercent(), rule.required(),
                rule.requiresPhysicalDelivery()
        );
        rule.documentRequirements().forEach(document -> collateral.addDocumentRequirement(
                new ApplicationCollateralDocumentRequirementEntity(
                        collateral, document.documentTypeId(), document.documentTypeCode(), document.documentTypeTitle(),
                        document.allowedContentTypes(), document.maximumSizeBytes(), document.required(),
                        document.minimumCount(), document.maximumCount(), document.displayOrder()
                )
        ));
        return collateral;
    }

    private void completeGuaranteeWhenReady(LoanApplicationEntity application) {
        boolean complete = application.getCollaterals().stream()
                .filter(ApplicationCollateralEntity::isRequired)
                .noneMatch(item -> item.getStatus() == ApplicationCollateralStatus.WAITING_PROVIDER
                        || item.getStatus() == ApplicationCollateralStatus.WAITING_INFORMATION);
        if (complete && application.getStatus() == ApplicationStatus.WAITING_GUARANTEE) {
            application.guaranteeInformationCompleted(
                    paymentService.allPaid(application.getId(), FeeCategory.APPLICATION)
            );
        }
    }
}
