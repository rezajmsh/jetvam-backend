package ir.jetvam.modules.product.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.modules.product.service.ProductCatalogService;
import ir.jetvam.modules.product.service.ProductViews;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Exposes the active customer-facing product catalog to authenticated user categories.
 * Disabled rules and inactive products or plans are never returned by this boundary.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
@RestController
@RequestMapping("/api/v1/catalog/products")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jetvam.product.api", name = "enabled", havingValue = "true", matchIfMissing = true)
@Tag(name = "Product catalog", description = "Active products, selectable plans and their applicable requirements.")
public class ProductCatalogController {

    private static final String CATALOG_ACCESS =
            "hasAnyRole('CUSTOMER', 'MERCHANT_USER', 'MERCHANT_ADMIN', 'MERCHANT_OPERATOR', "
                    + "'SYSTEM_ADMIN', 'SYSTEM_OPERATOR', 'SERVICE') "
                    + "and hasAuthority('product:catalog:read')";

    private final ProductCatalogService productCatalogService;

    @GetMapping
    @PreAuthorize(CATALOG_ACCESS)
    @Operation(summary = "List the active catalog", description = "Returns active products and their active customer-selectable plans.")
    public List<ProductViews.Product> catalog() {
        return productCatalogService.findActiveCatalog();
    }

    @GetMapping("/plans/{planId}")
    @PreAuthorize(CATALOG_ACCESS)
    @Operation(summary = "Get an active plan", description = "Returns current commercial values and enabled requirements for one selectable plan.")
    public ProductViews.Plan plan(@PathVariable UUID planId) {
        return productCatalogService.getActivePlan(planId);
    }
}
