package ir.jetvam.modules.integration.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.jetvam.modules.integration.http.DynamicProviderHttpClientFactory;
import ir.jetvam.modules.integration.routing.ProviderCircuitRegistry;
import ir.jetvam.modules.integration.routing.ProviderCircuitView;
import ir.jetvam.modules.integration.service.ExternalProviderView;
import ir.jetvam.modules.integration.service.ProviderConfigurationService;
import ir.jetvam.modules.integration.service.ProviderDefinitionCommand;
import ir.jetvam.modules.integration.service.ProviderRouteCommand;
import ir.jetvam.modules.integration.service.ProviderRouteView;
import ir.jetvam.modules.integration.service.TlsProfileCommand;
import ir.jetvam.modules.integration.service.TlsProfileView;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes role-and-permission protected administration for external service providers.
 * Runtime definitions, overrides and TLS profiles can be changed without application deployment.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
@Tag(name = "External providers", description = "Runtime routing, provider, circuit and TLS/mTLS administration.")
public class ProviderManagementController {

    private final ProviderConfigurationService configurationService;
    private final ProviderCircuitRegistry circuitRegistry;
    private final DynamicProviderHttpClientFactory clientFactory;

    @GetMapping("/routes")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR') and hasAuthority('integration:provider:read')")
    @Operation(summary = "List provider routes", description = "Returns routing policy and any active manual override per capability.")
    public List<ProviderRouteView> routes() {
        return configurationService.findRoutes();
    }

    @GetMapping("/providers")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR') and hasAuthority('integration:provider:read')")
    @Operation(summary = "List external providers", description = "Returns provider definitions, optionally filtered by capability.")
    public List<ExternalProviderView> providers(@RequestParam(required = false) String capability) {
        return configurationService.findProviders(capability);
    }

    @GetMapping("/tls-profiles")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR') and hasAuthority('integration:provider:read')")
    @Operation(summary = "List TLS profiles", description = "Returns runtime trust-store, key-store and protocol configuration without secrets.")
    public List<TlsProfileView> tlsProfiles() {
        return configurationService.findTlsProfiles();
    }

    @GetMapping("/circuits")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SYSTEM_OPERATOR') and hasAuthority('integration:provider:read')")
    @Operation(summary = "List provider circuits", description = "Returns in-memory failure and open-circuit state per capability and provider.")
    public List<ProviderCircuitView> circuits() {
        return circuitRegistry.snapshots();
    }

    @PutMapping("/routes/{capability}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and hasAuthority('integration:provider:write')")
    @Operation(summary = "Update a provider route", description = "Changes runtime routing, failover and circuit-breaker policy for a capability.")
    public ProviderRouteView updateRoute(
            @PathVariable String capability,
            @Valid @RequestBody UpdateProviderRouteRequest request
    ) {
        return configurationService.updateRoute(capability, new ProviderRouteCommand(
                request.routingMode(), request.failoverEnabled(), request.failureThreshold(),
                request.openDurationSeconds()
        ));
    }

    @PutMapping("/routes/{capability}/override")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and hasAuthority('integration:provider:override')")
    @Operation(summary = "Set a manual provider override", description = "Pins a capability to one provider until the optional expiry time.")
    public ProviderRouteView setOverride(
            @PathVariable String capability,
            @Valid @RequestBody SetProviderOverrideRequest request
    ) {
        return configurationService.setOverride(capability, request.providerCode(), request.until());
    }

    @DeleteMapping("/routes/{capability}/override")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and hasAuthority('integration:provider:override')")
    @Operation(summary = "Clear a manual provider override", description = "Returns a capability to its configured automatic routing policy.")
    public ProviderRouteView clearOverride(@PathVariable String capability) {
        return configurationService.clearOverride(capability);
    }

    @PutMapping("/providers/{capability}/{provider}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and hasAuthority('integration:provider:write')")
    @Operation(summary = "Create or update a provider", description = "Changes an adapter, endpoint, authentication, TLS and timeout configuration at runtime.")
    public ExternalProviderView upsertProvider(
            @PathVariable String capability,
            @PathVariable String provider,
            @Valid @RequestBody UpsertProviderRequest request
    ) {
        return configurationService.upsertProvider(capability, provider, new ProviderDefinitionCommand(
                request.adapterCode(), request.enabled(), request.priority(), request.weight(), request.baseUrl(),
                request.operationPath(), request.authenticationType(), request.authenticationHeader(),
                request.authenticationUsername(), request.credentialSecretRef(), request.tlsProfileCode(),
                request.connectTimeoutMillis(), request.readTimeoutMillis(), request.metadataJson()
        ));
    }

    @PutMapping("/tls-profiles/{profile}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and hasAuthority('integration:provider:write')")
    @Operation(summary = "Create or update a TLS profile", description = "Changes trust/key stores and protocols, then invalidates cached HTTP clients.")
    public TlsProfileView upsertTlsProfile(
            @PathVariable String profile,
            @Valid @RequestBody UpsertTlsProfileRequest request
    ) {
        TlsProfileView view = configurationService.upsertTlsProfile(profile, new TlsProfileCommand(
                request.storeType(), request.trustStoreLocation(), request.trustStorePasswordRef(),
                request.keyStoreLocation(), request.keyStorePasswordRef(), request.keyPasswordRef(),
                request.enabledProtocols()
        ));
        clientFactory.invalidateAll();
        return view;
    }

    @PostMapping("/circuits/{capability}/{provider}/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and hasAuthority('integration:provider:override')")
    @Operation(summary = "Reset a provider circuit", description = "Clears accumulated failures so the provider can be selected immediately.")
    public void resetCircuit(@PathVariable String capability, @PathVariable String provider) {
        circuitRegistry.reset(capability, provider);
    }

    @PostMapping("/clients/reload")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SYSTEM_ADMIN') and hasAuthority('integration:provider:override')")
    @Operation(summary = "Reload provider HTTP clients", description = "Invalidates all cached clients so changed TLS material and settings are re-read.")
    public void reloadClients() {
        clientFactory.invalidateAll();
    }
}
