package ir.jetvam.modules.product.service;

import ir.jetvam.common.exception.ConflictException;
import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.product.model.CollateralTypeEntity;
import ir.jetvam.modules.product.model.CollateralDocumentRequirementEntity;
import ir.jetvam.modules.product.model.DocumentTypeEntity;
import ir.jetvam.modules.product.model.FeeDefinitionEntity;
import ir.jetvam.modules.product.model.ProductFeeCategory;
import ir.jetvam.modules.product.model.ControlDefinitionEntity;
import ir.jetvam.modules.product.repository.CollateralTypeRepository;
import ir.jetvam.modules.product.repository.ControlDefinitionRepository;
import ir.jetvam.modules.product.repository.DocumentTypeRepository;
import ir.jetvam.modules.product.repository.FeeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.HashSet;
import java.util.UUID;

/**
 * Manages reusable collateral and fee master data independently from plan associations.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Service
@RequiredArgsConstructor
public class ProductReferenceDataService {

    private final CollateralTypeRepository collateralRepository;
    private final FeeDefinitionRepository feeRepository;
    private final ControlDefinitionRepository controlRepository;
    private final DocumentTypeRepository documentTypeRepository;

    @Transactional(readOnly = true)
    public ReferenceDataView findAll() {
        return new ReferenceDataView(
                collateralRepository.findAllByOrderByTitleAsc().stream().map(ProductReferenceDataService::view).toList(),
                feeRepository.findAllByOrderByTitleAsc().stream().map(ProductReferenceDataService::view).toList(),
                controlRepository.findAllByOrderByTitleAsc().stream().map(ProductReferenceDataService::view).toList(),
                documentTypeRepository.findAllByOrderByTitleAsc().stream().map(ProductReferenceDataService::view).toList()
        );
    }

    @Transactional
    public CollateralTypeView createCollateral(CollateralTypeCommand command) {
        String code = normalize(command.code());
        if (collateralRepository.existsByCode(code)) {
            throw new ConflictException("Collateral type code already exists: " + code);
        }
        CollateralTypeEntity entity = new CollateralTypeEntity(
                code, command.title(), command.handlerCode(), command.requiresPhysicalDelivery(),
                command.description(), command.active()
        );
        entity.replaceDocumentRequirements(requirements(entity, command.documentRequirements()));
        return view(collateralRepository.save(entity));
    }

    @Transactional
    public CollateralTypeView updateCollateral(UUID id, CollateralTypeCommand command) {
        CollateralTypeEntity entity = collateralRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("collateralType", id));
        entity.update(command.title(), command.handlerCode(), command.requiresPhysicalDelivery(),
                command.description(), command.active());
        entity.replaceDocumentRequirements(requirements(entity, command.documentRequirements()));
        return view(entity);
    }

    @Transactional
    public DocumentTypeView createDocumentType(DocumentTypeCommand command) {
        String code = normalize(command.code());
        if (documentTypeRepository.existsByCode(code)) {
            throw new ConflictException("Document type code already exists: " + code);
        }
        return view(documentTypeRepository.save(new DocumentTypeEntity(
                code, command.title(), command.allowedContentTypes(), command.maximumSizeBytes(), command.active()
        )));
    }

    @Transactional
    public DocumentTypeView updateDocumentType(UUID id, DocumentTypeCommand command) {
        DocumentTypeEntity entity = documentTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("documentType", id));
        entity.update(command.title(), command.allowedContentTypes(), command.maximumSizeBytes(), command.active());
        return view(entity);
    }

    @Transactional
    public FeeDefinitionView createFee(FeeDefinitionCommand command) {
        String code = normalize(command.code());
        if (feeRepository.existsByCode(code)) {
            throw new ConflictException("Fee definition code already exists: " + code);
        }
        return view(feeRepository.save(new FeeDefinitionEntity(
                code, command.title(), command.category(), command.amount(), command.currency(),
                command.triggerCode(), command.sourceInquiryCode(), command.refundable(), command.active()
        )));
    }

    @Transactional
    public FeeDefinitionView updateFee(UUID id, FeeDefinitionCommand command) {
        FeeDefinitionEntity entity = feeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("feeDefinition", id));
        entity.update(command.title(), command.category(), command.amount(), command.currency(),
                command.triggerCode(), command.sourceInquiryCode(), command.refundable(), command.active());
        return view(entity);
    }

    @Transactional
    public ControlDefinitionView updateControl(UUID id, ControlDefinitionCommand command) {
        ControlDefinitionEntity entity = controlRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("controlDefinition", id));
        entity.update(command.title(), command.defaultFailureMessage(), command.active());
        return view(entity);
    }

    private static String normalize(String value) {
        return Preconditions.requireText(value, "code").strip().toUpperCase();
    }

    private static CollateralTypeView view(CollateralTypeEntity entity) {
        return new CollateralTypeView(entity.getId(), entity.getCode(), entity.getTitle(), entity.getHandlerCode(),
                entity.isRequiresPhysicalDelivery(), entity.getDescription(), entity.isActive(),
                entity.getDocumentRequirements().stream()
                        .sorted(java.util.Comparator.comparingInt(CollateralDocumentRequirementEntity::getDisplayOrder))
                        .map(item -> new CollateralDocumentRequirementView(
                                item.getDocumentType().getId(), item.getDocumentType().getCode(),
                                item.getDocumentType().getTitle(), item.isRequired(), item.getMinimumCount(),
                                item.getMaximumCount(), item.getDisplayOrder()
                        )).toList());
    }

    private static DocumentTypeView view(DocumentTypeEntity entity) {
        return new DocumentTypeView(entity.getId(), entity.getCode(), entity.getTitle(),
                entity.getAllowedContentTypes(), entity.getMaximumSizeBytes(), entity.isActive());
    }

    private List<CollateralDocumentRequirementEntity> requirements(
            CollateralTypeEntity collateralType, List<CollateralDocumentRequirementCommand> commands
    ) {
        List<CollateralDocumentRequirementCommand> safeCommands = commands == null ? List.of() : commands;
        var ids = new HashSet<UUID>();
        return safeCommands.stream().map(command -> {
            Preconditions.require(ids.add(command.documentTypeId()), "A document type can be selected only once");
            DocumentTypeEntity documentType = documentTypeRepository.findById(command.documentTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("documentType", command.documentTypeId()));
            Preconditions.require(documentType.isActive(), "Selected document type is inactive");
            return new CollateralDocumentRequirementEntity(
                    collateralType, documentType, command.required(), command.minimumCount(),
                    command.maximumCount(), command.displayOrder()
            );
        }).toList();
    }

    private static FeeDefinitionView view(FeeDefinitionEntity entity) {
        return new FeeDefinitionView(entity.getId(), entity.getCode(), entity.getTitle(), entity.getCategory(),
                entity.getAmount(), entity.getCurrency(), entity.getTriggerCode(),
                entity.getSourceInquiryCode() == null ? null : entity.getSourceInquiryCode().code(),
                entity.isRefundable(), entity.isActive());
    }

    private static ControlDefinitionView view(ControlDefinitionEntity entity) {
        return new ControlDefinitionView(
                entity.getId(), entity.getCode(), entity.getTitle(), entity.getEvaluatorType(),
                entity.getInquiryCode() == null ? null : entity.getInquiryCode().code(),
                entity.getDefaultFailureMessage(), entity.isActive(),
                entity.getParameters().stream()
                        .sorted(java.util.Comparator.comparingInt(item -> item.getDisplayOrder()))
                        .map(item -> new ControlParameterView(
                                item.getId(), item.getCode(), item.getTitle(), item.getValueRole(),
                                item.getDataType(), item.isRequired(), item.getDisplayOrder()
                        )).toList()
        );
    }

    public record ReferenceDataView(
            List<CollateralTypeView> collateralTypes,
            List<FeeDefinitionView> feeDefinitions,
            List<ControlDefinitionView> controlDefinitions,
            List<DocumentTypeView> documentTypes
    ) {
    }

    public record CollateralTypeCommand(
            String code, String title, String handlerCode, boolean requiresPhysicalDelivery,
            String description, boolean active,
            List<CollateralDocumentRequirementCommand> documentRequirements
    ) {
    }

    public record CollateralTypeView(
            UUID id, String code, String title, String handlerCode, boolean requiresPhysicalDelivery,
            String description, boolean active, List<CollateralDocumentRequirementView> documentRequirements
    ) {
    }

    public record CollateralDocumentRequirementCommand(
            UUID documentTypeId, boolean required, int minimumCount, int maximumCount, int displayOrder
    ) {
    }

    public record CollateralDocumentRequirementView(
            UUID documentTypeId, String documentTypeCode, String documentTypeTitle, boolean required,
            int minimumCount, int maximumCount, int displayOrder
    ) {
    }

    public record DocumentTypeCommand(
            String code, String title, String allowedContentTypes, long maximumSizeBytes, boolean active
    ) {
    }

    public record DocumentTypeView(
            UUID id, String code, String title, String allowedContentTypes, long maximumSizeBytes, boolean active
    ) {
    }

    public record FeeDefinitionCommand(
            String code, String title, ProductFeeCategory category, BigDecimal amount, String currency,
            String triggerCode, InquiryType sourceInquiryCode, boolean refundable, boolean active
    ) {
    }

    public record FeeDefinitionView(
            UUID id, String code, String title, ProductFeeCategory category, BigDecimal amount, String currency,
            String triggerCode, String sourceInquiryCode, boolean refundable, boolean active
    ) {
    }

    public record ControlDefinitionCommand(String title, String defaultFailureMessage, boolean active) {
    }

    public record ControlDefinitionView(
            UUID id, String code, String title, ir.jetvam.modules.product.model.PlanControlType evaluatorType,
            String inquiryCode, String defaultFailureMessage, boolean active, List<ControlParameterView> parameters
    ) {
    }

    public record ControlParameterView(
            UUID id, String code, String title,
            ir.jetvam.modules.product.model.ControlParameterRole valueRole,
            ir.jetvam.modules.product.model.ControlParameterDataType dataType,
            boolean required, int displayOrder
    ) {
    }
}
