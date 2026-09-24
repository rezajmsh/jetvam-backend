package ir.jetvam.modules.integration.repository;

import ir.jetvam.modules.integration.persistence.TlsProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persists named TLS profiles shared by any number of external providers.
 * Profile version changes force dynamic HTTP clients to rebuild their SSL context.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
public interface TlsProfileRepository extends JpaRepository<TlsProfileEntity, UUID> {

    Optional<TlsProfileEntity> findByProfileCode(String profileCode);
}
