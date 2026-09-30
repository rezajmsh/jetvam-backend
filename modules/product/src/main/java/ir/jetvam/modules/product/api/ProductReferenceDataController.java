package ir.jetvam.modules.product.api;

import ir.jetvam.modules.product.model.ProductFeeCategory;
import ir.jetvam.common.inquiry.InquiryType;
import ir.jetvam.modules.product.service.ProductReferenceDataService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

/**
 * Exposes independent administration of collateral types and reusable fee definitions.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@RestController
@RequestMapping("/api/v1/admin/product-reference-data")
@RequiredArgsConstructor
public class ProductReferenceDataController {

    private static final String READ =
            "hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR') and hasAuthority('product:configuration:read')";
    private static final String WRITE =
            "hasRole('SYSTEM_ADMIN') and hasAuthority('product:configuration:write')";

    private final ProductReferenceDataService service;

    @GetMapping
    @PreAuthorize(READ)
    public ProductReferenceDataService.ReferenceDataView findAll() {
        return service.findAll();
    }

    @PostMapping("/collateral-types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(WRITE)
    public ProductReferenceDataService.CollateralTypeView createCollateral(
            @Valid @RequestBody CollateralTypeRequest request
    ) {
        return service.createCollateral(request.toCommand());
    }

    @PutMapping("/collateral-types/{id}")
    @PreAuthorize(WRITE)
    public ProductReferenceDataService.CollateralTypeView updateCollateral(
            @PathVariable UUID id, @Valid @RequestBody CollateralTypeRequest request
    ) {
        return service.updateCollateral(id, request.toCommand());
    }

    @PostMapping("/document-types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(WRITE)
    public ProductReferenceDataService.DocumentTypeView createDocumentType(
            @Valid @RequestBody DocumentTypeRequest request
    ) {
        return service.createDocumentType(request.toCommand());
    }

    @PutMapping("/document-types/{id}")
    @PreAuthorize(WRITE)
    public ProductReferenceDataService.DocumentTypeView updateDocumentType(
            @PathVariable UUID id, @Valid @RequestBody DocumentTypeRequest request
    ) {
        return service.updateDocumentType(id, request.toCommand());
    }

    @PostMapping("/fee-definitions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(WRITE)
    public ProductReferenceDataService.FeeDefinitionView createFee(@Valid @RequestBody FeeDefinitionRequest request) {
        return service.createFee(request.toCommand());
    }

    @PutMapping("/fee-definitions/{id}")
    @PreAuthorize(WRITE)
    public ProductReferenceDataService.FeeDefinitionView updateFee(
            @PathVariable UUID id, @Valid @RequestBody FeeDefinitionRequest request
    ) {
        return service.updateFee(id, request.toCommand());
    }

    @PutMapping("/control-definitions/{id}")
    @PreAuthorize(WRITE)
    public ProductReferenceDataService.ControlDefinitionView updateControl(
            @PathVariable UUID id, @Valid @RequestBody ControlDefinitionRequest request
    ) {
        return service.updateControl(id, new ProductReferenceDataService.ControlDefinitionCommand(
                request.title(), request.defaultFailureMessage(), request.active()
        ));
    }

    public record CollateralTypeRequest(
            @NotBlank @Size(max = 100) String code,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 100) String handlerCode,
            boolean requiresPhysicalDelivery,
            @Size(max = 1000) String description,
            boolean active,
            @Valid List<CollateralDocumentRequirementRequest> documentRequirements
    ) {
        ProductReferenceDataService.CollateralTypeCommand toCommand() {
            return new ProductReferenceDataService.CollateralTypeCommand(
                    code, title, handlerCode, requiresPhysicalDelivery, description, active,
                    documentRequirements == null ? List.of() : documentRequirements.stream()
                            .map(CollateralDocumentRequirementRequest::toCommand).toList()
            );
        }
    }

    public record CollateralDocumentRequirementRequest(
            @NotNull UUID documentTypeId,
            boolean required,
            @jakarta.validation.constraints.Min(0) int minimumCount,
            @jakarta.validation.constraints.Min(1) int maximumCount,
            @jakarta.validation.constraints.Min(1) int displayOrder
    ) {
        ProductReferenceDataService.CollateralDocumentRequirementCommand toCommand() {
            return new ProductReferenceDataService.CollateralDocumentRequirementCommand(
                    documentTypeId, required, minimumCount, maximumCount, displayOrder
            );
        }
    }

    public record DocumentTypeRequest(
            @NotBlank @Size(max = 100) String code,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 1000) String allowedContentTypes,
            @jakarta.validation.constraints.Positive long maximumSizeBytes,
            boolean active
    ) {
        ProductReferenceDataService.DocumentTypeCommand toCommand() {
            return new ProductReferenceDataService.DocumentTypeCommand(
                    code, title, allowedContentTypes, maximumSizeBytes, active
            );
        }
    }

    public record FeeDefinitionRequest(
            @NotBlank @Size(max = 100) String code,
            @NotBlank @Size(max = 200) String title,
            @NotNull ProductFeeCategory category,
            @NotNull @DecimalMin("0") BigDecimal amount,
            @NotBlank @Size(min = 3, max = 3) String currency,
            @NotBlank @Size(max = 100) String triggerCode,
            @Size(max = 100) String sourceInquiryCode,
            boolean refundable,
            boolean active
    ) {
        ProductReferenceDataService.FeeDefinitionCommand toCommand() {
            return new ProductReferenceDataService.FeeDefinitionCommand(
                    code, title, category, amount, currency, triggerCode,
                    sourceInquiryCode == null || sourceInquiryCode.isBlank()
                            ? null : InquiryType.fromCode(sourceInquiryCode),
                    refundable, active
            );
        }
    }

    public record ControlDefinitionRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 500) String defaultFailureMessage,
            boolean active
    ) {
    }
}
