package ir.jetvam.modules.product.model;

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
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final Set<PlanInquiryEntity> inquiries = new LinkedHashSet<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<PlanGuaranteeEntity> guarantees = new LinkedHashSet<>();

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
            Collection<PlanInquiryEntity> inquiries,
            Collection<PlanGuaranteeEntity> guarantees,
            Collection<PlanCollateralEntity> collaterals,
            Collection<PlanFeeEntity> fees,
            Collection<PlanControlEntity> controls
    ) {
        Collection<PlanInquiryEntity> safeInquiries = copy(inquiries, "inquiries");
        Collection<PlanGuaranteeEntity> safeGuarantees = copy(guarantees, "guarantees");
        Collection<PlanCollateralEntity> safeCollaterals = copy(collaterals, "collaterals");
        Collection<PlanFeeEntity> safeFees = copy(fees, "fees");
        Collection<PlanControlEntity> safeControls = copy(controls, "controls");

        requireUnique(safeInquiries, PlanInquiryEntity::getCode, "inquiry code");
        requireUnique(safeInquiries, PlanInquiryEntity::getSequence, "inquiry sequence");
        requireUnique(safeGuarantees, PlanGuaranteeEntity::getCode, "guarantee code");
        requireUnique(safeCollaterals, PlanCollateralEntity::getCode, "collateral code");
        requireUnique(safeFees, PlanFeeEntity::getCode, "fee code");
        requireUnique(safeControls, PlanControlEntity::getCode, "control code");
        requireUnique(safeControls, PlanControlEntity::getPriority, "control priority");

        Set<String> inquiryCodes = safeInquiries.stream().map(PlanInquiryEntity::getCode).collect(HashSet::new,
                Set::add, Set::addAll);
        Map<String, Boolean> enabledInquiries = safeInquiries.stream()
                .collect(Collectors.toMap(PlanInquiryEntity::getCode, PlanInquiryEntity::isEnabled));
        safeFees.stream()
                .map(PlanFeeEntity::getSourceInquiryCode)
                .filter(code -> code != null)
                .forEach(code -> Preconditions.require(inquiryCodes.contains(code),
                        "Fee sourceInquiryCode must reference an inquiry in the same plan: " + code));
        safeControls.stream()
                .map(PlanControlEntity::getSourceInquiryCode)
                .filter(code -> code != null)
                .forEach(code -> Preconditions.require(inquiryCodes.contains(code),
                        "Control sourceInquiryCode must reference an inquiry in the same plan: " + code));
        safeControls.stream()
                .filter(PlanControlEntity::isEnabled)
                .map(PlanControlEntity::getSourceInquiryCode)
                .filter(code -> code != null)
                .forEach(code -> Preconditions.require(Boolean.TRUE.equals(enabledInquiries.get(code)),
                        "Enabled control must reference an enabled inquiry: " + code));
        Set<String> controlledInquiryCodes = safeControls.stream()
                .filter(PlanControlEntity::isEnabled)
                .map(PlanControlEntity::getSourceInquiryCode)
                .filter(code -> code != null)
                .collect(Collectors.toSet());
        safeInquiries.stream()
                .filter(PlanInquiryEntity::isEnabled)
                .map(PlanInquiryEntity::getCode)
                .forEach(code -> Preconditions.require(controlledInquiryCodes.contains(code),
                        "Enabled inquiry must be referenced by an enabled control: " + code));

        replace(this.inquiries, safeInquiries);
        replace(this.guarantees, safeGuarantees);
        replace(this.collaterals, safeCollaterals);
        replace(this.fees, safeFees);
        replace(this.controls, safeControls);
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

    private static <T> void replace(Set<T> target, Collection<T> source) {
        target.clear();
        target.addAll(source);
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
