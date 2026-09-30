package ir.jetvam.modules.origination.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Represents one required or optional guarantor position snapshotted for a loan application.
 * The party link remains nullable until the identified individual is resolved or registers.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(
        name = "origination_application_guarantor",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_origination_guarantor_sequence", columnNames = {"application_id", "sequence_number"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationGuarantorEntity extends AbstractAuditableUuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplicationEntity application;
    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;
    @Column(name = "required", nullable = false)
    private boolean required;
    @Column(name = "party_id")
    private UUID partyId;
    @Column(name = "national_code", length = 10)
    private String nationalCode;
    @Column(name = "mobile", length = 11)
    private String mobile;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ApplicationGuarantorStatus status;

    public ApplicationGuarantorEntity(LoanApplicationEntity application, int sequenceNumber, boolean required) {
        this.application = Preconditions.requireNonNull(application, "application");
        this.sequenceNumber = Preconditions.requirePositive(sequenceNumber, "sequenceNumber");
        this.required = required;
        this.status = ApplicationGuarantorStatus.WAITING_IDENTIFICATION;
    }

    public void identify(String nationalCode, String mobile, UUID partyId) {
        this.nationalCode = Preconditions.requireText(nationalCode, "nationalCode").strip();
        this.mobile = Preconditions.requireText(mobile, "mobile").strip();
        this.partyId = partyId;
        this.status = ApplicationGuarantorStatus.WAITING_ACCEPTANCE;
    }
}
