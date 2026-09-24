package ir.jetvam.modules.integration.routing;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Indexes typed provider adapters by capability and adapter code at startup.
 * Duplicate registrations fail fast instead of producing nondeterministic routing.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Component
public class ProviderAdapterRegistry {

    private final Map<String, ExternalProviderAdapter<?, ?>> adapters;

    public ProviderAdapterRegistry(List<ExternalProviderAdapter<?, ?>> candidates) {
        Map<String, ExternalProviderAdapter<?, ?>> indexed = new HashMap<>();
        for (ExternalProviderAdapter<?, ?> adapter : candidates) {
            String key = key(adapter.capabilityCode(), adapter.adapterCode());
            if (indexed.putIfAbsent(key, adapter) != null) {
                throw new IllegalStateException("Duplicate external provider adapter " + key);
            }
        }
        adapters = Map.copyOf(indexed);
    }

    public ExternalProviderAdapter<?, ?> get(String capabilityCode, String adapterCode) {
        ExternalProviderAdapter<?, ?> adapter = adapters.get(key(capabilityCode, adapterCode));
        if (adapter == null) {
            throw new IllegalStateException("External provider adapter is not registered: " + adapterCode);
        }
        return adapter;
    }

    private static String key(String capabilityCode, String adapterCode) {
        return capabilityCode.strip().toUpperCase() + ":" + adapterCode.strip().toUpperCase();
    }
}
