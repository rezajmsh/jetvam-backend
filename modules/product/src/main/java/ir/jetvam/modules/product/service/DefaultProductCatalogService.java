package ir.jetvam.modules.product.service;

import ir.jetvam.common.exception.ConflictException;
import ir.jetvam.common.exception.FieldViolation;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.exception.ValidationException;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.product.model.PlanCollateralEntity;
import ir.jetvam.modules.product.model.PlanControlEntity;
import ir.jetvam.modules.product.model.PlanEntity;
import ir.jetvam.modules.product.model.PlanFeeEntity;
import ir.jetvam.modules.product.model.PlanGuarantorPolicyEntity;
import ir.jetvam.modules.product.model.PlanGuarantorCollateralEntity;
import ir.jetvam.modules.product.model.ProductEntity;
import ir.jetvam.modules.product.model.PublicationStatus;
import ir.jetvam.modules.product.repository.PlanRepository;
import ir.jetvam.modules.product.repository.ProductRepository;
import ir.jetvam.modules.product.repository.CollateralTypeRepository;
import ir.jetvam.modules.product.repository.FeeDefinitionRepository;
import ir.jetvam.modules.product.repository.ControlDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Implements product and plan lifecycle management behind a compact application boundary.
 * Active plans are immutable; administrators deactivate them before changing commercial rules.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Service
@RequiredArgsConstructor
public class DefaultProductCatalogService implements ProductCatalogService {

    private final ProductRepository productRepository;
    private final PlanRepository planRepository;
    private final CollateralTypeRepository collateralTypeRepository;
    private final FeeDefinitionRepository feeDefinitionRepository;
    private final ControlDefinitionRepository controlDefinitionRepository;

    @Override
    @Transactional
    public ProductViews.Product createProduct(ProductCommands.CreateProduct command) {
        Preconditions.requireNonNull(command, "command");
        String code = normalize(command.code());
        if (productRepository.existsByCode(code)) {
            throw new ConflictException("Product code already exists: " + code);
        }
        ProductEntity product = productRepository.save(new ProductEntity(code, command.name(), command.description()));
        return toProduct(product, List.of(), false);
    }

    @Override
    @Transactional
    public ProductViews.Product reviseProduct(UUID productId, ProductCommands.ReviseProduct command) {
        Preconditions.requireNonNull(command, "command");
        ProductEntity product = findProduct(productId);
        product.revise(command.name(), command.description());
        return toProduct(product, findPlans(productId), false);
    }

    @Override
    @Transactional
    public ProductViews.Product changeProductStatus(UUID productId, ProductCommands.ChangeStatus command) {
        Preconditions.requireNonNull(command, "command");
        ProductEntity product = findProduct(productId);
        product.changeStatus(command.status());
        return toProduct(product, findPlans(productId), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductViews.Product> findProducts() {
        return productRepository.findAllByOrderByNameAsc().stream()
                .map(product -> toProduct(product, findPlans(product.getId()), false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductViews.Product getProduct(UUID productId) {
        ProductEntity product = findProduct(productId);
        return toProduct(product, findPlans(productId), false);
    }

    @Override
    @Transactional
    public ProductViews.Plan createPlan(UUID productId, ProductCommands.CreatePlan command) {
        Preconditions.requireNonNull(command, "command");
        ProductEntity product = findProduct(productId);
        String code = normalize(command.code());
        if (planRepository.existsByProductIdAndCode(productId, code)) {
            throw new ConflictException("Plan code already exists for product: " + code);
        }
        PlanEntity plan = new PlanEntity(
                product, code, command.name(), command.description(), command.minimumAmount(), command.maximumAmount(),
                command.minimumTermMonths(), command.maximumTermMonths(), command.annualInterestRate()
        );
        return toPlan(planRepository.save(plan), false);
    }

    @Override
    @Transactional
    public ProductViews.Plan revisePlan(UUID planId, ProductCommands.RevisePlan command) {
        Preconditions.requireNonNull(command, "command");
        PlanEntity plan = findPlan(planId);
        requireEditable(plan);
        plan.revise(
                command.name(), command.description(), command.minimumAmount(), command.maximumAmount(),
                command.minimumTermMonths(), command.maximumTermMonths(), command.annualInterestRate()
        );
        return toPlan(plan, false);
    }

    @Override
    @Transactional
    public ProductViews.Plan changePlanStatus(UUID planId, ProductCommands.ChangeStatus command) {
        Preconditions.requireNonNull(command, "command");
        PlanEntity plan = findPlan(planId);
        if (command.status() == PublicationStatus.ACTIVE
                && plan.getProduct().getStatus() != PublicationStatus.ACTIVE) {
            throw new ConflictException("A plan can be activated only when its product is active");
        }
        plan.changeStatus(command.status());
        return toPlan(plan, false);
    }

    @Override
    @Transactional
    public ProductViews.Plan configurePlan(UUID planId, ProductCommands.ConfigurePlan command) {
        Preconditions.requireNonNull(command, "command");
        PlanEntity plan = findPlan(planId);
        requireEditable(plan);
        try {
            applyConfiguration(plan, command);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(
                    "Plan configuration is invalid",
                    List.of(new FieldViolation(
                            "configuration",
                            "PRODUCT.INVALID_PLAN_CONFIGURATION",
                            exception.getMessage()
                    ))
            );
        }
        planRepository.flush();
        return toPlan(plan, false);
    }

    private void applyConfiguration(PlanEntity plan, ProductCommands.ConfigurePlan command) {
        List<PlanGuarantorPolicyEntity> guarantorPolicies = command.guarantorPolicy() == null ? List.of() : List.of(
                new PlanGuarantorPolicyEntity(plan, command.guarantorPolicy().minimumCount(),
                        command.guarantorPolicy().maximumCount(), command.guarantorPolicy().required(),
                        command.guarantorPolicy().requiresCollateral(), command.guarantorPolicy().enabled())
        );
        List<PlanGuarantorCollateralEntity> guarantorCollaterals = command.guarantorCollaterals().stream()
                .map(rule -> new PlanGuarantorCollateralEntity(
                        plan, collateralTypeRepository.findById(rule.collateralTypeId())
                                .orElseThrow(() -> new ResourceNotFoundException("collateralType", rule.collateralTypeId())),
                        rule.minimumCoveragePercent(), rule.required(), rule.enabled()
                )).toList();
        List<PlanCollateralEntity> collaterals = command.collaterals().stream()
                .map(rule -> new PlanCollateralEntity(
                        plan, collateralTypeRepository.findById(rule.collateralTypeId())
                                .orElseThrow(() -> new ResourceNotFoundException("collateralType", rule.collateralTypeId())),
                        rule.minimumCoveragePercent(), rule.required(), rule.enabled()
                )).toList();
        List<PlanFeeEntity> fees = command.fees().stream()
                .map(rule -> new PlanFeeEntity(
                        plan, feeDefinitionRepository.findById(rule.feeDefinitionId())
                                .orElseThrow(() -> new ResourceNotFoundException("feeDefinition", rule.feeDefinitionId())),
                        rule.enabled()
                )).toList();
        List<PlanControlEntity> controls = command.controls().stream()
                .map(rule -> {
                    var definition = controlDefinitionRepository.findById(rule.controlDefinitionId())
                            .orElseThrow(() -> new ResourceNotFoundException(
                                    "controlDefinition", rule.controlDefinitionId()));
                    Preconditions.require(definition.isActive(), "Selected control definition is inactive");
                    return new PlanControlEntity(
                            plan, definition, rule.subjectType(), rule.priority(), rule.enabled(),
                            rule.parameters().stream().map(value -> new PlanControlEntity.ParameterValue(
                                    value.parameterDefinitionId(), value.numericValue()
                            )).toList()
                    );
                }).toList();
        plan.replaceConfiguration(guarantorPolicies, guarantorCollaterals, collaterals, fees, controls);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductViews.Product> findActiveCatalog() {
        return productRepository.findAllByStatusOrderByNameAsc(PublicationStatus.ACTIVE).stream()
                .map(product -> toProduct(
                        product,
                        planRepository.findAllByProductIdAndStatusOrderByNameAsc(product.getId(), PublicationStatus.ACTIVE),
                        true
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductViews.Plan getActivePlan(UUID planId) {
        PlanEntity plan = planRepository.findByIdAndStatus(planId, PublicationStatus.ACTIVE)
                .filter(candidate -> candidate.getProduct().getStatus() == PublicationStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("activePlan", planId));
        return toPlan(plan, true);
    }

    private ProductEntity findProduct(UUID productId) {
        return productRepository.findById(Preconditions.requireNonNull(productId, "productId"))
                .orElseThrow(() -> new ResourceNotFoundException("product", productId));
    }

    private PlanEntity findPlan(UUID planId) {
        return planRepository.findById(Preconditions.requireNonNull(planId, "planId"))
                .orElseThrow(() -> new ResourceNotFoundException("plan", planId));
    }

    private List<PlanEntity> findPlans(UUID productId) {
        return planRepository.findAllByProductIdOrderByNameAsc(productId);
    }

    private static void requireEditable(PlanEntity plan) {
        if (plan.getStatus() == PublicationStatus.ACTIVE) {
            throw new ConflictException("Active plans must be deactivated before their configuration is changed");
        }
    }

    private static ProductViews.Product toProduct(
            ProductEntity product,
            List<PlanEntity> plans,
            boolean publicView
    ) {
        return new ProductViews.Product(
                product.getId(), product.getCode(), product.getName(), product.getDescription(), product.getStatus(),
                product.getVersion(), plans.stream().map(plan -> toPlan(plan, publicView)).toList()
        );
    }

    private static ProductViews.Plan toPlan(PlanEntity plan, boolean publicView) {
        return new ProductViews.Plan(
                plan.getId(), plan.getProduct().getId(), plan.getCode(), plan.getName(), plan.getDescription(),
                plan.getMinimumAmount(), plan.getMaximumAmount(), plan.getMinimumTermMonths(),
                plan.getMaximumTermMonths(), plan.getAnnualInterestRate(), plan.getStatus(), plan.getVersion(),
                plan.getGuarantorPolicies().stream()
                        .filter(item -> !publicView || item.isEnabled())
                        .findFirst()
                        .map(item -> new ProductViews.GuarantorPolicy(
                                item.getMinimumCount(), item.getMaximumCount(), item.isRequired(),
                                item.isRequiresCollateral(), item.isEnabled()
                        )).orElse(null),
                plan.getGuarantorCollaterals().stream()
                        .filter(item -> !publicView || item.isEnabled())
                        .sorted(Comparator.comparing(item -> item.getCollateralType().getCode()))
                        .map(item -> collateralView(item.getCollateralType(), item.getMinimumCoveragePercent(),
                                item.isRequired(), item.isEnabled()))
                        .toList(),
                plan.getCollaterals().stream()
                        .filter(item -> !publicView || item.isEnabled())
                        .sorted(Comparator.comparing(item -> item.getCollateralType().getCode()))
                        .map(item -> collateralView(item.getCollateralType(), item.getMinimumCoveragePercent(),
                                item.isRequired(), item.isEnabled()))
                        .toList(),
                plan.getFees().stream()
                        .filter(item -> !publicView || item.isEnabled())
                        .sorted(Comparator.comparing(item -> item.getFeeDefinition().getCode()))
                        .map(item -> new ProductViews.Fee(
                                item.getFeeDefinition().getId(), item.getFeeDefinition().getCode(),
                                item.getFeeDefinition().getTitle(), item.getFeeDefinition().getAmount(),
                                item.getFeeDefinition().getCurrency(), item.getFeeDefinition().getTriggerCode(),
                                item.getFeeDefinition().getSourceInquiryCode() == null ? null
                                        : item.getFeeDefinition().getSourceInquiryCode().code(),
                                item.getFeeDefinition().isRefundable(),
                                item.isEnabled()
                        )).toList(),
                plan.getControls().stream()
                        .filter(item -> !publicView || item.isEnabled())
                        .sorted(Comparator.comparingInt(PlanControlEntity::getPriority))
                        .map(item -> new ProductViews.Control(
                                item.getControlDefinition().getId(), item.getControlDefinition().getCode(),
                                item.getControlDefinition().getTitle(), item.getPriority(),
                                item.getControlDefinition().getEvaluatorType(), item.getSubjectType(),
                                item.value(ir.jetvam.modules.product.model.ControlParameterRole.MINIMUM),
                                item.value(ir.jetvam.modules.product.model.ControlParameterRole.MAXIMUM),
                                item.getControlDefinition().getInquiryCode() == null ? null
                                        : item.getControlDefinition().getInquiryCode().code(),
                                item.getControlDefinition().getDefaultFailureMessage(),
                                item.getControlDefinition().getParameters().stream()
                                        .sorted(Comparator.comparingInt(
                                                ir.jetvam.modules.product.model.ControlParameterDefinitionEntity::getDisplayOrder
                                        ))
                                        .map(definition -> new ProductViews.ControlParameter(
                                                definition.getId(), definition.getCode(), definition.getTitle(),
                                                definition.getValueRole(), definition.getDataType(), definition.isRequired(),
                                                item.getParameterValues().stream()
                                                        .filter(value -> value.getParameterDefinition().getId()
                                                                .equals(definition.getId()))
                                                        .map(ir.jetvam.modules.product.model.PlanControlParameterValueEntity::getNumericValue)
                                                        .findFirst().orElse(null)
                                        )).toList(),
                                item.isEnabled()
                        )).toList()
        );
    }

    private static String normalize(String value) {
        return Preconditions.requireText(value, "code").strip().toUpperCase();
    }

    private static ProductViews.Collateral collateralView(
            ir.jetvam.modules.product.model.CollateralTypeEntity collateralType,
            java.math.BigDecimal coverage, boolean required, boolean enabled
    ) {
        return new ProductViews.Collateral(
                collateralType.getId(), collateralType.getCode(), collateralType.getTitle(),
                collateralType.getHandlerCode(), collateralType.isRequiresPhysicalDelivery(), coverage,
                required, enabled, collateralType.getDocumentRequirements().stream()
                        .sorted(Comparator.comparingInt(
                                ir.jetvam.modules.product.model.CollateralDocumentRequirementEntity::getDisplayOrder))
                        .map(item -> new ProductViews.DocumentRequirement(
                                item.getDocumentType().getId(), item.getDocumentType().getCode(),
                                item.getDocumentType().getTitle(), item.getDocumentType().getAllowedContentTypes(),
                                item.getDocumentType().getMaximumSizeBytes(), item.isRequired(), item.getMinimumCount(),
                                item.getMaximumCount(), item.getDisplayOrder()
                        )).toList()
        );
    }
}
