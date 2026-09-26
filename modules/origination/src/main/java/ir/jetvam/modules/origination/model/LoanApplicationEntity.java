package ir.jetvam.modules.origination.model;

import ir.jetvam.common.exception.IllegalStateTransitionException;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Owns the immutable plan snapshot and state machine of one customer loan application.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@Entity
@Table(name = "origination_loan_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoanApplicationEntity extends AbstractAuditableUuidEntity {

    @Column(name = "customer_party_id", nullable = false)
    private UUID customerPartyId;

    @Column(name = "national_code", nullable = false, length = 10)
    private String nationalCode;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Column(name = "plan_code", nullable = false, length = 80)
    private String planCode;

    @Column(name = "plan_name", nullable = false, length = 200)
    private String planName;

    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Column(name = "annual_interest_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal annualInterestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ApplicationStatus status;

    @Column(name = "personal_profile_revision")
    private Long personalProfileRevision;

    @Column(name = "employment_profile_revision")
    private Long employmentProfileRevision;

    @Column(name = "guarantee_information_json", columnDefinition = "text")
    private String guaranteeInformationJson;

    @Column(name = "requires_guarantee", nullable = false)
    private boolean requiresGuarantee;

    @Column(name = "requires_original_cheque", nullable = false)
    private boolean requiresOriginalCheque;

    @Column(name = "requires_application_fee", nullable = false)
    private boolean requiresApplicationFee;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "contract_signed_at")
    private Instant contractSignedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private final Set<ApplicationControlEntity> controls = new LinkedHashSet<>();

    public LoanApplicationEntity(
            UUID customerPartyId,
            String nationalCode,
            LocalDate birthDate,
            UUID planId,
            String planCode,
            String planName,
            BigDecimal requestedAmount,
            int termMonths,
            BigDecimal annualInterestRate,
            boolean requiresGuarantee,
            boolean requiresOriginalCheque,
            boolean requiresApplicationFee,
            Long personalProfileRevision,
            Long employmentProfileRevision,
            ApplicationStatus initialStatus
    ) {
        this.customerPartyId = Preconditions.requireNonNull(customerPartyId, "customerPartyId");
        this.nationalCode = Preconditions.requireText(nationalCode, "nationalCode");
        this.birthDate = Preconditions.requireNonNull(birthDate, "birthDate");
        this.planId = Preconditions.requireNonNull(planId, "planId");
        this.planCode = Preconditions.requireText(planCode, "planCode");
        this.planName = Preconditions.requireText(planName, "planName");
        this.requestedAmount = Preconditions.requireNonNull(requestedAmount, "requestedAmount");
        this.termMonths = Preconditions.requirePositive(termMonths, "termMonths");
        this.annualInterestRate = Preconditions.requireNonNull(annualInterestRate, "annualInterestRate");
        this.requiresGuarantee = requiresGuarantee;
        this.requiresOriginalCheque = requiresOriginalCheque;
        this.requiresApplicationFee = requiresApplicationFee;
        this.personalProfileRevision = positiveRevisionOrNull(personalProfileRevision, "personalProfileRevision");
        this.employmentProfileRevision = positiveRevisionOrNull(employmentProfileRevision, "employmentProfileRevision");
        this.status = Preconditions.requireNonNull(initialStatus, "initialStatus");
    }

    public void addControl(ApplicationControlEntity control) {
        controls.add(Preconditions.requireNonNull(control, "control"));
    }

    public void controlsPaid() {
        transition(ApplicationStatus.WAITING_CONTROL_FEE, ApplicationStatus.WAITING_CONTROLS);
    }

    public void waitForControlFee() {
        if (status == ApplicationStatus.WAITING_CONTROLS) {
            status = ApplicationStatus.WAITING_CONTROL_FEE;
        } else {
            requireStatus(ApplicationStatus.WAITING_CONTROL_FEE);
        }
    }

    public void controlsPassed() {
        requireStatus(ApplicationStatus.WAITING_CONTROLS);
        advanceFromProfileInformation(false);
    }

    public void cancelRemainingControls() {
        controls.forEach(ApplicationControlEntity::cancel);
    }

    public void reject(String reason) {
        this.rejectionReason = Preconditions.requireText(reason, "reason").strip();
        this.status = ApplicationStatus.REJECTED;
    }

    public void requireManualReview(String reason) {
        this.rejectionReason = Preconditions.requireText(reason, "reason").strip();
        this.status = ApplicationStatus.MANUAL_REVIEW;
    }

    public void usePersonalProfile(
            long personalRevision,
            Long availableEmploymentRevision,
            boolean applicationFeesPaid
    ) {
        requireStatus(ApplicationStatus.WAITING_PERSONAL_INFORMATION);
        this.personalProfileRevision = Preconditions.requirePositive(personalRevision, "personalRevision");
        this.employmentProfileRevision = positiveRevisionOrNull(
                availableEmploymentRevision,
                "availableEmploymentRevision"
        );
        advanceFromProfileInformation(applicationFeesPaid);
    }

    public void useEmploymentProfile(long employmentRevision, boolean applicationFeesPaid) {
        requireStatus(ApplicationStatus.WAITING_EMPLOYMENT_INFORMATION);
        this.employmentProfileRevision = Preconditions.requirePositive(employmentRevision, "employmentRevision");
        advanceFromProfileInformation(applicationFeesPaid);
    }

    public void adoptAvailableProfileInformation(
            Long availablePersonalRevision,
            Long availableEmploymentRevision,
            boolean applicationFeesPaid
    ) {
        if (status == ApplicationStatus.WAITING_PERSONAL_INFORMATION && availablePersonalRevision != null) {
            this.personalProfileRevision = positiveRevisionOrNull(
                    availablePersonalRevision,
                    "availablePersonalRevision"
            );
        }
        if ((status == ApplicationStatus.WAITING_PERSONAL_INFORMATION
                || status == ApplicationStatus.WAITING_EMPLOYMENT_INFORMATION)
                && availableEmploymentRevision != null) {
            this.employmentProfileRevision = positiveRevisionOrNull(
                    availableEmploymentRevision,
                    "availableEmploymentRevision"
            );
        }
        if (status == ApplicationStatus.WAITING_PERSONAL_INFORMATION
                || status == ApplicationStatus.WAITING_EMPLOYMENT_INFORMATION) {
            advanceFromProfileInformation(applicationFeesPaid);
        }
    }

    public void recordGuaranteeInformation(String json, boolean applicationFeesPaid) {
        requireStatus(ApplicationStatus.WAITING_GUARANTEE);
        this.guaranteeInformationJson = Preconditions.requireText(json, "guaranteeInformationJson");
        afterGuarantee(applicationFeesPaid);
    }

    public void applicationFeesPaid() {
        requireStatus(ApplicationStatus.WAITING_APPLICATION_FEE);
        this.status = requiresOriginalCheque
                ? ApplicationStatus.WAITING_ORIGINAL_CHEQUE
                : ApplicationStatus.WAITING_SIGNATURE;
    }

    public void originalChequeReceived() {
        transition(ApplicationStatus.WAITING_ORIGINAL_CHEQUE, ApplicationStatus.WAITING_SIGNATURE);
    }

    public void signContract(Instant at) {
        requireStatus(ApplicationStatus.WAITING_SIGNATURE);
        this.contractSignedAt = Preconditions.requireNonNull(at, "at");
        this.status = ApplicationStatus.WAITING_CREDIT_ALLOCATION;
    }

    public void allocateCredit(Instant at) {
        requireStatus(ApplicationStatus.WAITING_CREDIT_ALLOCATION);
        this.completedAt = Preconditions.requireNonNull(at, "at");
        this.status = ApplicationStatus.COMPLETED;
    }

    private void afterGuarantee(boolean applicationFeesPaid) {
        if (requiresApplicationFee && !applicationFeesPaid) {
            this.status = ApplicationStatus.WAITING_APPLICATION_FEE;
        } else {
            this.status = requiresOriginalCheque
                    ? ApplicationStatus.WAITING_ORIGINAL_CHEQUE
                    : ApplicationStatus.WAITING_SIGNATURE;
        }
    }

    private void advanceFromProfileInformation(boolean applicationFeesPaid) {
        if (personalProfileRevision == null) {
            this.status = ApplicationStatus.WAITING_PERSONAL_INFORMATION;
        } else if (employmentProfileRevision == null) {
            this.status = ApplicationStatus.WAITING_EMPLOYMENT_INFORMATION;
        } else if (requiresGuarantee) {
            this.status = ApplicationStatus.WAITING_GUARANTEE;
        } else {
            afterGuarantee(applicationFeesPaid);
        }
    }

    private static Long positiveRevisionOrNull(Long revision, String field) {
        return revision == null ? null : Preconditions.requirePositive(revision, field);
    }

    private void transition(ApplicationStatus expected, ApplicationStatus target) {
        requireStatus(expected);
        this.status = target;
    }

    private void requireStatus(ApplicationStatus expected) {
        if (status != expected) {
            throw new IllegalStateTransitionException(status, expected);
        }
    }
}
