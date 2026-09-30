package ir.jetvam.modules.origination.model;

import ir.jetvam.common.validation.Preconditions;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Stores typed Sayad cheque information for a collateral handled by the cheque workflow.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(name = "origination_application_cheque_collateral")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationChequeCollateralEntity {

    @Id
    @Column(name = "application_collateral_id")
    private UUID applicationCollateralId;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_collateral_id")
    private ApplicationCollateralEntity collateral;
    @Column(name = "sayad_id", nullable = false, length = 32)
    private String sayadId;
    @Column(name = "bank_code", nullable = false, length = 20)
    private String bankCode;
    @Column(name = "cheque_number", nullable = false, length = 50)
    private String chequeNumber;
    @Column(name = "cheque_serial", nullable = false, length = 50)
    private String chequeSerial;
    @Column(name = "cheque_date", nullable = false)
    private LocalDate chequeDate;
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    public ApplicationChequeCollateralEntity(
            ApplicationCollateralEntity collateral, String sayadId, String bankCode, String chequeNumber,
            String chequeSerial, LocalDate chequeDate, BigDecimal amount
    ) {
        this.collateral = Preconditions.requireNonNull(collateral, "collateral");
        update(sayadId, bankCode, chequeNumber, chequeSerial, chequeDate, amount);
    }

    public void update(
            String sayadId, String bankCode, String chequeNumber,
            String chequeSerial, LocalDate chequeDate, BigDecimal amount
    ) {
        this.sayadId = Preconditions.requireText(sayadId, "sayadId").strip();
        this.bankCode = Preconditions.requireText(bankCode, "bankCode").strip();
        this.chequeNumber = Preconditions.requireText(chequeNumber, "chequeNumber").strip();
        this.chequeSerial = Preconditions.requireText(chequeSerial, "chequeSerial").strip();
        this.chequeDate = Preconditions.requireNonNull(chequeDate, "chequeDate");
        this.amount = Preconditions.requireNonNull(amount, "amount");
        Preconditions.require(amount.signum() > 0, "amount must be positive");
    }
}
