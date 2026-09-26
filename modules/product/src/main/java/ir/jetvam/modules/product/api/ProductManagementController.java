package ir.jetvam.modules.product.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.modules.product.model.PublicationStatus;
import ir.jetvam.modules.product.model.PlanControlType;
import ir.jetvam.modules.product.service.ProductCatalogService;
import ir.jetvam.modules.product.service.ProductCommands;
import ir.jetvam.modules.product.service.ProductViews;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Exposes role-and-permission protected administration of products and plan rules.
 * A full configuration replacement keeps plan requirements internally consistent and auditable.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jetvam.product.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Product administration", description = "Product, plan and per-plan requirement configuration.")
public class ProductManagementController {

    private static final String READ_ACCESS =
            "hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR') and hasAuthority('product:configuration:read')";
    private static final String WRITE_ACCESS =
            "hasRole('SYSTEM_ADMIN') and hasAuthority('product:configuration:write')";

    private final ProductCatalogService productCatalogService;

    @GetMapping
    @PreAuthorize(READ_ACCESS)
    @Operation(summary = "List products", description = "Returns every product and plan, including draft and disabled configuration.")
    public List<ProductViews.Product> products() {
        return productCatalogService.findProducts();
    }

    @GetMapping("/{productId}")
    @PreAuthorize(READ_ACCESS)
    @Operation(summary = "Get a product", description = "Returns one product with its complete administrative plan configuration.")
    public ProductViews.Product product(@PathVariable UUID productId) {
        return productCatalogService.getProduct(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(WRITE_ACCESS)
    @Operation(summary = "Create a product", description = "Creates a draft product family under which selectable plans can be defined.")
    public ProductViews.Product createProduct(@Valid @RequestBody ProductRequest request) {
        return productCatalogService.createProduct(new ProductCommands.CreateProduct(
                request.code(), request.name(), request.description()
        ));
    }

    @PutMapping("/{productId}")
    @PreAuthorize(WRITE_ACCESS)
    @Operation(summary = "Update a product", description = "Changes the product's customer-facing name and description.")
    public ProductViews.Product reviseProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRevisionRequest request
    ) {
        return productCatalogService.reviseProduct(productId, new ProductCommands.ReviseProduct(
                request.name(), request.description()
        ));
    }

    @PatchMapping("/{productId}/status")
    @PreAuthorize(WRITE_ACCESS)
    @Operation(summary = "Change product status", description = "Publishes, unpublishes or returns a product to draft state.")
    public ProductViews.Product changeProductStatus(
            @PathVariable UUID productId,
            @Valid @RequestBody StatusRequest request
    ) {
        return productCatalogService.changeProductStatus(productId, new ProductCommands.ChangeStatus(request.status()));
    }

    @PostMapping("/{productId}/plans")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(WRITE_ACCESS)
    @Operation(summary = "Create a plan", description = "Creates a customer-selectable draft plan under the product.")
    public ProductViews.Plan createPlan(
            @PathVariable UUID productId,
            @Valid @RequestBody PlanRequest request
    ) {
        return productCatalogService.createPlan(productId, request.toCreateCommand());
    }

    @PutMapping("/plans/{planId}")
    @PreAuthorize(WRITE_ACCESS)
    @Operation(summary = "Update a plan", description = "Changes commercial values of a non-active plan.")
    public ProductViews.Plan revisePlan(@PathVariable UUID planId, @Valid @RequestBody PlanRevisionRequest request) {
        return productCatalogService.revisePlan(planId, request.toCommand());
    }

    @PatchMapping("/plans/{planId}/status")
    @PreAuthorize(WRITE_ACCESS)
    @Operation(summary = "Change plan status", description = "Activates or deactivates a plan; its parent product must be active before activation.")
    public ProductViews.Plan changePlanStatus(
            @PathVariable UUID planId,
            @Valid @RequestBody StatusRequest request
    ) {
        return productCatalogService.changePlanStatus(planId, new ProductCommands.ChangeStatus(request.status()));
    }

    @PutMapping("/plans/{planId}/configuration")
    @PreAuthorize(WRITE_ACCESS)
    @Operation(summary = "Configure a plan", description = "Atomically replaces inquiry, guarantor, collateral and fee rules of a non-active plan.")
    public ProductViews.Plan configurePlan(
            @PathVariable UUID planId,
            @Valid @RequestBody PlanConfigurationRequest request
    ) {
        return productCatalogService.configurePlan(planId, request.toCommand());
    }

    public record ProductRequest(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description
    ) {
    }

    public record ProductRevisionRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description
    ) {
    }

    public record StatusRequest(@NotNull PublicationStatus status) {
    }

    public record PlanRequest(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @NotNull @DecimalMin("0") BigDecimal minimumAmount,
            @NotNull @DecimalMin("0") BigDecimal maximumAmount,
            @Positive int minimumTermMonths,
            @Positive int maximumTermMonths,
            @NotNull @DecimalMin("0") BigDecimal annualInterestRate
    ) {
        ProductCommands.CreatePlan toCreateCommand() {
            return new ProductCommands.CreatePlan(
                    code, name, description, minimumAmount, maximumAmount, minimumTermMonths, maximumTermMonths,
                    annualInterestRate
            );
        }
    }

    public record PlanRevisionRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description,
            @NotNull @DecimalMin("0") BigDecimal minimumAmount,
            @NotNull @DecimalMin("0") BigDecimal maximumAmount,
            @Positive int minimumTermMonths,
            @Positive int maximumTermMonths,
            @NotNull @DecimalMin("0") BigDecimal annualInterestRate
    ) {
        ProductCommands.RevisePlan toCommand() {
            return new ProductCommands.RevisePlan(
                    name, description, minimumAmount, maximumAmount, minimumTermMonths, maximumTermMonths,
                    annualInterestRate
            );
        }
    }

    public record PlanConfigurationRequest(
            @Valid List<InquiryRequest> inquiries,
            @Valid List<GuaranteeRequest> guarantees,
            @Valid List<CollateralRequest> collaterals,
            @Valid List<FeeRequest> fees,
            @Valid List<ControlRequest> controls
    ) {
        ProductCommands.ConfigurePlan toCommand() {
            return new ProductCommands.ConfigurePlan(
                    inquiries == null ? List.of() : inquiries.stream().map(InquiryRequest::toCommand).toList(),
                    guarantees == null ? List.of() : guarantees.stream().map(GuaranteeRequest::toCommand).toList(),
                    collaterals == null ? List.of() : collaterals.stream().map(CollateralRequest::toCommand).toList(),
                    fees == null ? List.of() : fees.stream().map(FeeRequest::toCommand).toList(),
                    controls == null ? List.of() : controls.stream().map(ControlRequest::toCommand).toList()
            );
        }
    }

    public record InquiryRequest(
            @NotBlank @Size(max = 100) String code,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 100) String stageCode,
            @Positive int sequence,
            boolean required,
            boolean enabled,
            String configurationJson
    ) {
        ProductCommands.InquiryRule toCommand() {
            return new ProductCommands.InquiryRule(
                    code, title, stageCode, sequence, required, enabled, configurationJson
            );
        }
    }

    public record GuaranteeRequest(
            @NotBlank @Size(max = 100) String code,
            @NotBlank @Size(max = 200) String title,
            @PositiveOrZero int minimumCount,
            @PositiveOrZero int maximumCount,
            boolean required,
            boolean enabled,
            String configurationJson
    ) {
        ProductCommands.GuaranteeRule toCommand() {
            return new ProductCommands.GuaranteeRule(
                    code, title, minimumCount, maximumCount, required, enabled, configurationJson
            );
        }
    }

    public record CollateralRequest(
            @NotBlank @Size(max = 100) String code,
            @NotBlank @Size(max = 200) String title,
            @NotNull @DecimalMin("0") BigDecimal minimumCoveragePercent,
            boolean required,
            boolean enabled,
            String configurationJson
    ) {
        ProductCommands.CollateralRule toCommand() {
            return new ProductCommands.CollateralRule(
                    code, title, minimumCoveragePercent, required, enabled, configurationJson
            );
        }
    }

    public record FeeRequest(
            @NotBlank @Size(max = 100) String code,
            @NotBlank @Size(max = 200) String title,
            @NotNull @DecimalMin("0") BigDecimal amount,
            @NotBlank @Size(min = 3, max = 3) String currency,
            @NotBlank @Size(max = 100) String triggerCode,
            @Size(max = 100) String sourceInquiryCode,
            boolean refundable,
            boolean enabled
    ) {
        ProductCommands.FeeRule toCommand() {
            return new ProductCommands.FeeRule(
                    code, title, amount, currency, triggerCode, sourceInquiryCode, refundable, enabled
            );
        }
    }

    public record ControlRequest(
            @NotBlank @Size(max = 100) String code,
            @NotBlank @Size(max = 200) String title,
            @Positive int priority,
            @NotNull PlanControlType type,
            @DecimalMin("0") BigDecimal minimumValue,
            @DecimalMin("0") BigDecimal maximumValue,
            @Size(max = 100) String sourceInquiryCode,
            @NotBlank @Size(max = 500) String failureMessage,
            boolean enabled
    ) {
        ProductCommands.ControlRule toCommand() {
            return new ProductCommands.ControlRule(
                    code, title, priority, type, minimumValue, maximumValue, sourceInquiryCode, failureMessage, enabled
            );
        }
    }
}
