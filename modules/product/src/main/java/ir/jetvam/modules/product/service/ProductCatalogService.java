package ir.jetvam.modules.product.service;

import java.util.List;
import java.util.UUID;

/**
 * Defines the product module's narrow application boundary for administration and catalog reads.
 * Other business modules consume these provider-independent views instead of product persistence.
 *
 * @author reza jamshidi
 * @since 9/25/2026
 */
public interface ProductCatalogService {

    ProductViews.Product createProduct(ProductCommands.CreateProduct command);

    ProductViews.Product reviseProduct(UUID productId, ProductCommands.ReviseProduct command);

    ProductViews.Product changeProductStatus(UUID productId, ProductCommands.ChangeStatus command);

    List<ProductViews.Product> findProducts();

    ProductViews.Product getProduct(UUID productId);

    ProductViews.Plan createPlan(UUID productId, ProductCommands.CreatePlan command);

    ProductViews.Plan revisePlan(UUID planId, ProductCommands.RevisePlan command);

    ProductViews.Plan changePlanStatus(UUID planId, ProductCommands.ChangeStatus command);

    ProductViews.Plan configurePlan(UUID planId, ProductCommands.ConfigurePlan command);

    List<ProductViews.Product> findActiveCatalog();

    ProductViews.Plan getActivePlan(UUID planId);
}
