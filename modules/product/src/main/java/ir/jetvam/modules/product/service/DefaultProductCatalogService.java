package ir.jetvam.modules.product.service;

import ir.jetvam.common.exception.ConflictException;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.product.model.PlanCollateralEntity;
import ir.jetvam.modules.product.model.PlanControlEntity;
import ir.jetvam.modules.product.model.PlanEntity;
import ir.jetvam.modules.product.model.PlanFeeEntity;
import ir.jetvam.modules.product.model.PlanGuaranteeEntity;
import ir.jetvam.modules.product.model.PlanInquiryEntity;
import ir.jetvam.modules.product.model.ProductEntity;
import ir.jetvam.modules.product.model.PublicationStatus;
import ir.jetvam.modules.product.repository.PlanRepository;
import ir.jetvam.modules.product.repository.ProductRepository;
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
        List<PlanInquiryEntity> inquiries = command.inquiries().stream()
                .map(rule -> new PlanInquiryEntity(
                        plan, rule.code(), rule.title(), rule.stageCode(), rule.sequence(), rule.required(),
                        rule.enabled(), rule.configurationJson()
                )).toList();
        List<PlanGuaranteeEntity> guarantees = command.guarantees().stream()
                .map(rule -> new PlanGuaranteeEntity(
                        plan, rule.code(), rule.title(), rule.minimumCount(), rule.maximumCount(), rule.required(),
                        rule.enabled(), rule.configurationJson()
                )).toList();
        List<PlanCollateralEntity> collaterals = command.collaterals().stream()
                .map(rule -> new PlanCollateralEntity(
                        plan, rule.code(), rule.title(), rule.minimumCoveragePercent(), rule.required(), rule.enabled(),
                        rule.configurationJson()
                )).toList();
        List<PlanFeeEntity> fees = command.fees().stream()
                .map(rule -> new PlanFeeEntity(
                        plan, rule.code(), rule.title(), rule.amount(), rule.currency(), rule.triggerCode(),
                        rule.sourceInquiryCode(), rule.refundable(), rule.enabled()
                )).toList();
        List<PlanControlEntity> controls = command.controls().stream()
                .map(rule -> new PlanControlEntity(
                        plan, rule.code(), rule.title(), rule.priority(), rule.type(), rule.minimumValue(), rule.maximumValue(),
                        rule.sourceInquiryCode(), rule.failureMessage(), rule.enabled()
                )).toList();
        plan.replaceConfiguration(inquiries, guarantees, collaterals, fees, controls);
        planRepository.flush();
        return toPlan(plan, false);
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
                plan.getInquiries().stream()
                        .filter(item -> !publicView || item.isEnabled())
                        .sorted(Comparator.comparingInt(PlanInquiryEntity::getSequence))
                        .map(item -> new ProductViews.Inquiry(
                                item.getCode(), item.getTitle(), item.getStageCode(), item.getSequence(),
                                item.isRequired(), item.isEnabled(), item.getConfigurationJson()
                        )).toList(),
                plan.getGuarantees().stream()
                        .filter(item -> !publicView || item.isEnabled())
                        .sorted(Comparator.comparing(PlanGuaranteeEntity::getCode))
                        .map(item -> new ProductViews.Guarantee(
                                item.getCode(), item.getTitle(), item.getMinimumCount(), item.getMaximumCount(),
                                item.isRequired(), item.isEnabled(), item.getConfigurationJson()
                        )).toList(),
                plan.getCollaterals().stream()
                        .filter(item -> !publicView || item.isEnabled())
                        .sorted(Comparator.comparing(PlanCollateralEntity::getCode))
                        .map(item -> new ProductViews.Collateral(
                                item.getCode(), item.getTitle(), item.getMinimumCoveragePercent(), item.isRequired(),
                                item.isEnabled(), item.getConfigurationJson()
                        )).toList(),
                plan.getFees().stream()
                        .filter(item -> !publicView || item.isEnabled())
                        .sorted(Comparator.comparing(PlanFeeEntity::getCode))
                        .map(item -> new ProductViews.Fee(
                                item.getCode(), item.getTitle(), item.getAmount(), item.getCurrency(),
                                item.getTriggerCode(), item.getSourceInquiryCode(), item.isRefundable(), item.isEnabled()
                        )).toList(),
                plan.getControls().stream()
                        .filter(item -> !publicView || item.isEnabled())
                        .sorted(Comparator.comparingInt(PlanControlEntity::getPriority))
                        .map(item -> new ProductViews.Control(
                                item.getCode(), item.getTitle(), item.getPriority(), item.getType(), item.getMinimumValue(),
                                item.getMaximumValue(), item.getSourceInquiryCode(), item.getFailureMessage(),
                                item.isEnabled()
                        )).toList()
        );
    }

    private static String normalize(String value) {
        return Preconditions.requireText(value, "code").strip().toUpperCase();
    }
}
