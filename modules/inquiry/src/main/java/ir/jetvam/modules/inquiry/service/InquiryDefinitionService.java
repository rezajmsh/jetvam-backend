package ir.jetvam.modules.inquiry.service;

import ir.jetvam.common.exception.ConfigurationException;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.inquiry.model.InquiryDefinitionEntity;
import ir.jetvam.modules.inquiry.repository.InquiryDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * Manages per-capability enablement and result-validity policy.
 * Execution services consult this database-backed policy before using a provider or cached result.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
@Service
@RequiredArgsConstructor
public class InquiryDefinitionService {

    private final InquiryDefinitionRepository repository;

    @Transactional(readOnly = true)
    public Duration requireEnabledValidity(InquiryType inquiryCode) {
        InquiryDefinitionEntity definition = find(inquiryCode);
        if (!definition.isEnabled()) {
            throw new ConfigurationException("Inquiry capability is disabled: " + inquiryCode);
        }
        return definition.validity();
    }

    @Transactional(readOnly = true)
    public Duration validity(InquiryType inquiryCode) {
        return find(inquiryCode).validity();
    }

    @Transactional(readOnly = true)
    public List<DefinitionView> findAll() {
        return repository.findAll().stream()
                .map(InquiryDefinitionService::view)
                .sorted(java.util.Comparator.comparing(DefinitionView::inquiryCode))
                .toList();
    }

    @Transactional
    public DefinitionView update(
            InquiryType inquiryCode,
            Duration validity,
            boolean enabled,
            boolean requiresSubjectOtp
    ) {
        Preconditions.requireNonNull(validity, "validity");
        InquiryDefinitionEntity definition = find(inquiryCode);
        definition.update(validity, enabled, requiresSubjectOtp);
        return view(definition);
    }

    private InquiryDefinitionEntity find(InquiryType inquiryCode) {
        InquiryType type = Preconditions.requireNonNull(inquiryCode, "inquiryCode");
        return repository.findByInquiryCode(type)
                .orElseThrow(() -> new ResourceNotFoundException("inquiryDefinition", type.code()));
    }

    private static DefinitionView view(InquiryDefinitionEntity definition) {
        return new DefinitionView(
                definition.getInquiryCode().code(),
                definition.getTitle(),
                definition.getValiditySeconds(),
                definition.isEnabled(),
                definition.isRequiresSubjectOtp()
        );
    }

    public record DefinitionView(
            String inquiryCode,
            String title,
            long validitySeconds,
            boolean enabled,
            boolean requiresSubjectOtp
    ) {
    }
}
