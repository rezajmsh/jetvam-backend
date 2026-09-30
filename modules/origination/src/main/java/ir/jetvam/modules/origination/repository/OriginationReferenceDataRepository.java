package ir.jetvam.modules.origination.repository;

import ir.jetvam.modules.origination.model.OriginationReferenceDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Reads active application reference options by arbitrary categories.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
public interface OriginationReferenceDataRepository extends JpaRepository<OriginationReferenceDataEntity, UUID> {
    List<OriginationReferenceDataEntity> findAllByCategoryInAndActiveTrueOrderByCategoryAscDisplayOrderAsc(
            Collection<String> categories
    );
}
