package ir.jetvam.modules.origination.repository;

import ir.jetvam.modules.origination.model.ApplicationChequeCollateralEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Persists typed cheque information associated with an application collateral requirement.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public interface ApplicationChequeCollateralRepository
        extends JpaRepository<ApplicationChequeCollateralEntity, UUID> {
}
