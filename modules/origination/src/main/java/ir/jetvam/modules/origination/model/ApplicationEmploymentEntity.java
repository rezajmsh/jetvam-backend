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
import java.util.UUID;

/**
 * Stores mutable education, occupation and income facts as an application snapshot.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(name = "origination_application_employment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationEmploymentEntity {

    @Id
    @Column(name = "application_id")
    private UUID applicationId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private LoanApplicationEntity application;

    @Column(name = "education_code", nullable = false, length = 100)
    private String educationCode;

    @Column(name = "employment_code", nullable = false, length = 100)
    private String employmentCode;

    @Column(name = "monthly_income", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyIncome;

    public ApplicationEmploymentEntity(
            LoanApplicationEntity application,
            String educationCode,
            String employmentCode,
            BigDecimal monthlyIncome
    ) {
        this.application = Preconditions.requireNonNull(application, "application");
        update(educationCode, employmentCode, monthlyIncome);
    }

    public void update(String educationCode, String employmentCode, BigDecimal monthlyIncome) {
        this.educationCode = normalize(educationCode, "educationCode");
        this.employmentCode = normalize(employmentCode, "employmentCode");
        this.monthlyIncome = Preconditions.requireNonNull(monthlyIncome, "monthlyIncome");
        Preconditions.require(monthlyIncome.signum() >= 0, "monthlyIncome must not be negative");
    }

    private static String normalize(String value, String field) {
        return Preconditions.requireText(value, field).strip().toUpperCase();
    }
}
