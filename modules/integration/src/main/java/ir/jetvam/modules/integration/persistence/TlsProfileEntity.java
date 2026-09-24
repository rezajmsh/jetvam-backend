package ir.jetvam.modules.integration.persistence;

import ir.jetvam.common.text.TextUtils;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stores a named, runtime-changeable TLS and mutual-TLS profile.
 * Key material remains in external files or secret stores referenced by this record.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Entity
@Table(name = "integration_tls_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TlsProfileEntity extends AbstractAuditableUuidEntity {

    @Column(name = "profile_code", nullable = false, unique = true, length = 100)
    private String profileCode;

    @Column(name = "store_type", nullable = false, length = 30)
    private String storeType;

    @Column(name = "trust_store_location", length = 1000)
    private String trustStoreLocation;

    @Column(name = "trust_store_password_ref", length = 1000)
    private String trustStorePasswordRef;

    @Column(name = "key_store_location", length = 1000)
    private String keyStoreLocation;

    @Column(name = "key_store_password_ref", length = 1000)
    private String keyStorePasswordRef;

    @Column(name = "key_password_ref", length = 1000)
    private String keyPasswordRef;

    @Column(name = "enabled_protocols", nullable = false, length = 200)
    private String enabledProtocols;

    public TlsProfileEntity(String profileCode) {
        this.profileCode = normalize(profileCode);
    }

    public void configure(
            String storeType,
            String trustStoreLocation,
            String trustStorePasswordRef,
            String keyStoreLocation,
            String keyStorePasswordRef,
            String keyPasswordRef,
            String enabledProtocols
    ) {
        this.storeType = Preconditions.requireText(storeType, "storeType").strip().toUpperCase();
        this.trustStoreLocation = stripToNull(trustStoreLocation);
        this.trustStorePasswordRef = stripToNull(trustStorePasswordRef);
        this.keyStoreLocation = stripToNull(keyStoreLocation);
        this.keyStorePasswordRef = stripToNull(keyStorePasswordRef);
        this.keyPasswordRef = stripToNull(keyPasswordRef);
        this.enabledProtocols = Preconditions.requireText(enabledProtocols, "enabledProtocols").strip();
        if (this.keyStoreLocation != null) {
            Preconditions.requireText(this.keyStorePasswordRef, "keyStorePasswordRef");
        }
    }

    private static String normalize(String value) {
        return Preconditions.requireText(value, "profileCode").strip().toUpperCase();
    }

    private static String stripToNull(String value) {
        return TextUtils.hasText(value) ? value.strip() : null;
    }
}
