package ir.jetvam.modules.origination.repository;

import ir.jetvam.modules.origination.model.ApplicationEmploymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Persists application-owned education and employment snapshots.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public interface ApplicationEmploymentRepository extends JpaRepository<ApplicationEmploymentEntity, UUID> {
}
