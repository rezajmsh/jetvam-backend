package ir.jetvam.modules.product.model;

import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.BiConsumer;

/**
 * Represents the commercial offer selected by a customer within a product.
 * It is the aggregate root for commercial values, eligibility controls and fulfillment requirements.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Entity
@Table(
        name = "product_catalog_plan",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_plan_code", columnNames = {"product_id", "code"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanEntity extends AbstractAuditableUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "minimum_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal minimumAmount;

    @Column(name = "maximum_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal maximumAmount;

    @Column(name = "minimum_term_months", nullable = false)
    private int minimumTermMonths;

    @Column(name = "maximum_term_months", nullable = false)
    private int maximumTermMonths;

    @Column(name = "annual_interest_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal annualInterestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PublicationStatus status;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<PlanGuarantorPolicyEntity> guarantorPolicies = new LinkedHashSet<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<PlanGuarantorCollateralEntity> guarantorCollaterals = new LinkedHashSet<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<PlanCollateralEntity> collaterals = new LinkedHashSet<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<PlanFeeEntity> fees = new LinkedHashSet<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<PlanControlEntity> controls = new LinkedHashSet<>();

    public PlanEntity(
            ProductEntity product,
            String code,
            String name,
            String description,
            BigDecimal minimumAmount,
            BigDecimal maximumAmount,
            int minimumTermMonths,
            int maximumTermMonths,
            BigDecimal annualInterestRate
    ) {
        this.product = Preconditions.requireNonNull(product, "product");
        this.code = normalizeCode(code);
        revise(name, description, minimumAmount, maximumAmount, minimumTermMonths, maximumTermMonths,
                annualInterestRate);
        this.status = PublicationStatus.DRAFT;
    }

    public void revise(
            String name,
            String description,
            BigDecimal minimumAmount,
            BigDecimal maximumAmount,
            int minimumTermMonths,
            int maximumTermMonths,
            BigDecimal annualInterestRate
    ) {
        this.name = Preconditions.requireText(name, "name").strip();
        this.description = description == null || description.isBlank() ? null : description.strip();
        this.minimumAmount = requireNonNegative(minimumAmount, "minimumAmount");
        this.maximumAmount = requireNonNegative(maximumAmount, "maximumAmount");
        Preconditions.require(maximumAmount.compareTo(minimumAmount) >= 0,
                "maximumAmount must not be less than minimumAmount");
        this.minimumTermMonths = Preconditions.requirePositive(minimumTermMonths, "minimumTermMonths");
        this.maximumTermMonths = Preconditions.requirePositive(maximumTermMonths, "maximumTermMonths");
        Preconditions.require(maximumTermMonths >= minimumTermMonths,
                "maximumTermMonths must not be less than minimumTermMonths");
        this.annualInterestRate = requireNonNegative(annualInterestRate, "annualInterestRate");
    }

    public void replaceConfiguration(
            Collection<PlanGuarantorPolicyEntity> guarantorPolicies,
            Collection<PlanGuarantorCollateralEntity> guarantorCollaterals,
            Collection<PlanCollateralEntity> collaterals,
            Collection<PlanFeeEntity> fees,
            Collection<PlanControlEntity> controls
    ) {
        Collection<PlanGuarantorPolicyEntity> safeGuarantorPolicies = copy(guarantorPolicies, "guarantorPolicies");
        Collection<PlanGuarantorCollateralEntity> safeGuarantorCollaterals = copy(guarantorCollaterals, "guarantorCollaterals");
        Collection<PlanCollateralEntity> safeCollaterals = copy(collaterals, "collaterals");
        Collection<PlanFeeEntity> safeFees = copy(fees, "fees");
        Collection<PlanControlEntity> safeControls = copy(controls, "controls");

        Preconditions.require(safeGuarantorPolicies.size() <= 1, "A plan can have at most one guarantor policy");
        requireUnique(safeGuarantorCollaterals, item -> item.getCollateralType().getId(), "guarantor collateral type");
        requireUnique(safeCollaterals, item -> item.getCollateralType().getId(), "collateral type");
        requireUnique(safeFees, item -> item.getFeeDefinition().getId(), "fee definition");
        requireUnique(safeControls,
                item -> item.getSubjectType() + ":" + item.getControlDefinition().getId(),
                "control definition for subject");
        requireUnique(safeControls, item -> item.getSubjectType() + ":" + item.getPriority(),
                "control priority for subject");
        Set<InquiryType> inquiryCodes = safeControls.stream()
                .filter(PlanControlEntity::isEnabled)
                .map(item -> item.getControlDefinition().getInquiryCode())
                .filter(java.util.Objects::nonNull)
                .collect(HashSet::new, Set::add, Set::addAll);
        safeFees.stream()
                .map(item -> item.getFeeDefinition().getSourceInquiryCode())
                .filter(code -> code != null)
                .forEach(code -> Preconditions.require(inquiryCodes.contains(code),
                        "Fee sourceInquiryCode must belong to an enabled control in the same plan: " + code));
        boolean guarantorCollateralRequired = safeGuarantorPolicies.stream()
                .anyMatch(policy -> policy.isEnabled() && policy.isRequiresCollateral());
        Preconditions.require(!guarantorCollateralRequired || safeGuarantorCollaterals.stream()
                        .anyMatch(PlanGuarantorCollateralEntity::isEnabled),
                "A guarantor policy requiring collateral must select at least one guarantor collateral type");
        synchronize(this.guarantorPolicies, safeGuarantorPolicies, ignored -> "POLICY",
                (target, source) -> target.reconfigure(
                        source.getMinimumCount(), source.getMaximumCount(), source.isRequired(),
                        source.isRequiresCollateral(), source.isEnabled()
                ));
        synchronize(this.guarantorCollaterals, safeGuarantorCollaterals,
                item -> item.getCollateralType().getId(),
                (target, source) -> target.reconfigure(
                        source.getMinimumCoveragePercent(), source.isRequired(), source.isEnabled()
                ));
        synchronize(this.collaterals, safeCollaterals, item -> item.getCollateralType().getId(),
                (target, source) -> target.reconfigure(
                        source.getMinimumCoveragePercent(), source.isRequired(), source.isEnabled()
                ));
        synchronize(this.fees, safeFees, item -> item.getFeeDefinition().getId(),
                (target, source) -> target.reconfigure(source.isEnabled()));
        synchronize(this.controls, safeControls,
                item -> item.getSubjectType() + ":" + item.getControlDefinition().getId(),
                (target, source) -> target.reconfigure(
                        source.getPriority(), source.isEnabled(), source.getParameterValues().stream()
                                .map(value -> new PlanControlEntity.ParameterValue(
                                        value.getParameterDefinition().getId(), value.getNumericValue()
                                )).toList()
                ));
    }

    public void changeStatus(PublicationStatus status) {
        this.status = Preconditions.requireNonNull(status, "status");
    }

    private static <T> Collection<T> copy(Collection<T> source, String name) {
        return new LinkedHashSet<>(Preconditions.requireNonNull(source, name));
    }

    private static <T, K> void requireUnique(Collection<T> values, Function<T, K> key, String label) {
        Set<K> unique = new HashSet<>();
        values.forEach(value -> Preconditions.require(unique.add(key.apply(value)), "Duplicate " + label));
    }

    private static <T, K> void synchronize(
            Set<T> target,
            Collection<T> source,
            Function<T, K> key,
            BiConsumer<T, T> merge
    ) {
        java.util.Map<K, T> existing = target.stream()
                .collect(java.util.stream.Collectors.toMap(key, Function.identity()));
        Set<K> requestedKeys = source.stream().map(key).collect(java.util.stream.Collectors.toSet());
        target.removeIf(item -> !requestedKeys.contains(key.apply(item)));
        source.forEach(incoming -> {
            T current = existing.get(key.apply(incoming));
            if (current == null) {
                target.add(incoming);
            } else {
                merge.accept(current, incoming);
            }
        });
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String name) {
        Preconditions.requireNonNull(value, name);
        Preconditions.require(value.signum() >= 0, name + " must not be negative");
        return value;
    }

    private static String normalizeCode(String value) {
        return Preconditions.requireText(value, "code").strip().toUpperCase();
    }
}
