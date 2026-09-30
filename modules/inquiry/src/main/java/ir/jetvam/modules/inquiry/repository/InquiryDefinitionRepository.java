package ir.jetvam.modules.inquiry.repository;

import ir.jetvam.infra.persistence.repository.JetvamJpaRepository;
import ir.jetvam.modules.inquiry.model.InquiryDefinitionEntity;
import ir.jetvam.common.inquiry.InquiryType;

import java.util.Optional;
import java.util.UUID;

/**
 * Loads mutable inquiry validity policies by their stable capability code.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
public interface InquiryDefinitionRepository extends JetvamJpaRepository<InquiryDefinitionEntity, UUID> {

    Optional<InquiryDefinitionEntity> findByInquiryCode(InquiryType inquiryCode);
}
