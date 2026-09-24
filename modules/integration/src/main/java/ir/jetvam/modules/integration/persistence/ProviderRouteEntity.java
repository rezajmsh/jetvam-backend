package ir.jetvam.modules.integration.persistence;

import ir.jetvam.common.text.TextUtils;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import ir.jetvam.modules.integration.model.ProviderRoutingMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Stores runtime routing policy and optional manual override for one capability.
 * Optimistic locking makes concurrent administrative changes explicit and safe.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Entity
@Table(name = "integration_provider_route")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProviderRouteEntity extends AbstractAuditableUuidEntity {

    @Column(name = "capability_code", nullable = false, unique = true, length = 100)
    private String capabilityCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "routing_mode", nullable = false, length = 30)
    private ProviderRoutingMode routingMode;

    @Column(name = "failover_enabled", nullable = false)
    private boolean failoverEnabled;

    @Column(name = "forced_provider_code", length = 100)
    private String forcedProviderCode;

    @Column(name = "forced_until")
    private Instant forcedUntil;

    @Column(name = "failure_threshold", nullable = false)
    private int failureThreshold;

    @Column(name = "open_duration_seconds", nullable = false)
    private long openDurationSeconds;

    public ProviderRouteEntity(String capabilityCode) {
        this.capabilityCode = normalize(capabilityCode, "capabilityCode");
        changePolicy(ProviderRoutingMode.PRIORITY_FAILOVER, true, 3, 30);
    }

    public void changePolicy(
            ProviderRoutingMode routingMode,
            boolean failoverEnabled,
            int failureThreshold,
            long openDurationSeconds
    ) {
        this.routingMode = Preconditions.requireNonNull(routingMode, "routingMode");
        this.failoverEnabled = failoverEnabled;
        this.failureThreshold = Preconditions.requirePositive(failureThreshold, "failureThreshold");
        this.openDurationSeconds = Preconditions.requirePositive(openDurationSeconds, "openDurationSeconds");
    }

    public void forceProvider(String providerCode, Instant until) {
        this.forcedProviderCode = normalize(providerCode, "providerCode");
        this.forcedUntil = Preconditions.requireNonNull(until, "until");
        Preconditions.require(until.isAfter(Instant.now()), "Provider override expiry must be in the future");
    }

    public void clearOverride() {
        forcedProviderCode = null;
        forcedUntil = null;
    }

    public boolean hasActiveOverride(Instant now) {
        return TextUtils.hasText(forcedProviderCode) && forcedUntil != null && forcedUntil.isAfter(now);
    }

    private static String normalize(String value, String name) {
        return Preconditions.requireText(value, name).strip().toUpperCase();
    }
}
