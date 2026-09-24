package ir.jetvam.modules.integration.service;

import ir.jetvam.common.exception.ResourceNotFoundException;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.integration.persistence.ExternalProviderEntity;
import ir.jetvam.modules.integration.persistence.ProviderRouteEntity;
import ir.jetvam.modules.integration.persistence.TlsProfileEntity;
import ir.jetvam.modules.integration.repository.ExternalProviderRepository;
import ir.jetvam.modules.integration.repository.ProviderRouteRepository;
import ir.jetvam.modules.integration.repository.TlsProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Manages provider, route and TLS configuration shared by all external capabilities.
 * Runtime reads remain uncached so database changes are visible without restarting applications.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Service
@RequiredArgsConstructor
public class ProviderConfigurationService {

    private final ProviderRouteRepository routeRepository;
    private final ExternalProviderRepository providerRepository;
    private final TlsProfileRepository tlsProfileRepository;

    @Transactional(readOnly = true)
    public List<ProviderRouteView> findRoutes() {
        return routeRepository.findAll().stream().map(ProviderConfigurationService::toView).toList();
    }

    @Transactional(readOnly = true)
    public ProviderRouteView getRoute(String capabilityCode) {
        return toView(findRoute(capabilityCode));
    }

    @Transactional(readOnly = true)
    public List<ExternalProviderView> findProviders(String capabilityCode) {
        if (capabilityCode == null || capabilityCode.isBlank()) {
            return providerRepository.findAll().stream().map(ProviderConfigurationService::toView).toList();
        }
        return providerRepository.findAllByCapabilityCodeOrderByPriorityAscProviderCodeAsc(normalize(capabilityCode))
                .stream().map(ProviderConfigurationService::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<ExternalProviderView> findEnabledProviders(String capabilityCode) {
        return providerRepository.findAllByCapabilityCodeAndEnabledTrueOrderByPriorityAscProviderCodeAsc(
                normalize(capabilityCode)
        ).stream().map(ProviderConfigurationService::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<TlsProfileView> findTlsProfiles() {
        return tlsProfileRepository.findAll().stream().map(ProviderConfigurationService::toView).toList();
    }

    @Transactional(readOnly = true)
    public Optional<TlsProfileView> findTlsProfile(String profileCode) {
        if (profileCode == null || profileCode.isBlank()) {
            return Optional.empty();
        }
        return tlsProfileRepository.findByProfileCode(normalize(profileCode)).map(ProviderConfigurationService::toView);
    }

    @Transactional
    public ProviderRouteView updateRoute(String capabilityCode, ProviderRouteCommand command) {
        Preconditions.requireNonNull(command, "command");
        String capability = normalize(capabilityCode);
        ProviderRouteEntity route = routeRepository.findByCapabilityCode(capability)
                .orElseGet(() -> new ProviderRouteEntity(capability));
        route.changePolicy(command.routingMode(), command.failoverEnabled(), command.failureThreshold(),
                command.openDurationSeconds());
        return toView(routeRepository.saveAndFlush(route));
    }

    @Transactional
    public ProviderRouteView setOverride(String capabilityCode, String providerCode, Instant until) {
        String capability = normalize(capabilityCode);
        String provider = normalize(providerCode);
        ExternalProviderEntity definition = providerRepository.findByCapabilityCodeAndProviderCode(capability, provider)
                .orElseThrow(() -> new ResourceNotFoundException("externalProvider", capability + ":" + provider));
        Preconditions.require(definition.isEnabled(), "Forced provider must be enabled");
        ProviderRouteEntity route = findRoute(capability);
        route.forceProvider(provider, until);
        return toView(routeRepository.saveAndFlush(route));
    }

    @Transactional
    public ProviderRouteView clearOverride(String capabilityCode) {
        ProviderRouteEntity route = findRoute(capabilityCode);
        route.clearOverride();
        return toView(routeRepository.saveAndFlush(route));
    }

    @Transactional
    public ExternalProviderView upsertProvider(
            String capabilityCode,
            String providerCode,
            ProviderDefinitionCommand command
    ) {
        Preconditions.requireNonNull(command, "command");
        String capability = normalize(capabilityCode);
        String provider = normalize(providerCode);
        if (command.tlsProfileCode() != null && !command.tlsProfileCode().isBlank()) {
            String tlsCode = normalize(command.tlsProfileCode());
            tlsProfileRepository.findByProfileCode(tlsCode)
                    .orElseThrow(() -> new ResourceNotFoundException("tlsProfile", tlsCode));
        }
        ProviderRouteEntity route = routeRepository.findByCapabilityCode(capability)
                .orElseGet(() -> routeRepository.saveAndFlush(new ProviderRouteEntity(capability)));
        ExternalProviderEntity entity = providerRepository.findByCapabilityCodeAndProviderCode(capability, provider)
                .orElseGet(() -> new ExternalProviderEntity(route.getCapabilityCode(), provider));
        entity.configure(
                command.adapterCode(), command.enabled(), command.priority(), command.weight(), command.baseUrl(),
                command.operationPath(), command.authenticationType(), command.authenticationHeader(),
                command.authenticationUsername(), command.credentialSecretRef(), command.tlsProfileCode(),
                command.connectTimeoutMillis(), command.readTimeoutMillis(), command.metadataJson()
        );
        return toView(providerRepository.saveAndFlush(entity));
    }

    @Transactional
    public TlsProfileView upsertTlsProfile(String profileCode, TlsProfileCommand command) {
        Preconditions.requireNonNull(command, "command");
        String code = normalize(profileCode);
        TlsProfileEntity entity = tlsProfileRepository.findByProfileCode(code)
                .orElseGet(() -> new TlsProfileEntity(code));
        entity.configure(
                command.storeType(), command.trustStoreLocation(), command.trustStorePasswordRef(),
                command.keyStoreLocation(), command.keyStorePasswordRef(), command.keyPasswordRef(),
                command.enabledProtocols()
        );
        return toView(tlsProfileRepository.saveAndFlush(entity));
    }

    private ProviderRouteEntity findRoute(String capabilityCode) {
        String capability = normalize(capabilityCode);
        return routeRepository.findByCapabilityCode(capability)
                .orElseThrow(() -> new ResourceNotFoundException("providerRoute", capability));
    }

    private static ProviderRouteView toView(ProviderRouteEntity entity) {
        return new ProviderRouteView(
                entity.getCapabilityCode(), entity.getRoutingMode(), entity.isFailoverEnabled(),
                entity.getForcedProviderCode(), entity.getForcedUntil(), entity.getFailureThreshold(),
                entity.getOpenDurationSeconds(), entity.getVersion()
        );
    }

    private static ExternalProviderView toView(ExternalProviderEntity entity) {
        return new ExternalProviderView(
                entity.getCapabilityCode(), entity.getProviderCode(), entity.getAdapterCode(), entity.isEnabled(),
                entity.getPriority(), entity.getWeight(), entity.getBaseUrl(), entity.getOperationPath(),
                entity.getAuthenticationType(), entity.getAuthenticationHeader(), entity.getAuthenticationUsername(),
                entity.getCredentialSecretRef(), entity.getTlsProfileCode(), entity.getConnectTimeoutMillis(),
                entity.getReadTimeoutMillis(), entity.getMetadataJson(), entity.getVersion()
        );
    }

    private static TlsProfileView toView(TlsProfileEntity entity) {
        return new TlsProfileView(
                entity.getProfileCode(), entity.getStoreType(), entity.getTrustStoreLocation(),
                entity.getTrustStorePasswordRef(), entity.getKeyStoreLocation(), entity.getKeyStorePasswordRef(),
                entity.getKeyPasswordRef(), entity.getEnabledProtocols(), entity.getVersion()
        );
    }

    private static String normalize(String value) {
        return Preconditions.requireText(value, "code").strip().toUpperCase();
    }
}
