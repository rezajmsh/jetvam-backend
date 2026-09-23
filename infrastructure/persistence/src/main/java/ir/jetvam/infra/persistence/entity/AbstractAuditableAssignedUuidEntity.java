package ir.jetvam.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Provides auditing and optimistic locking for entities with an externally derived UUID.
 * It is intended for shared-primary-key dependents mapped through {@code @MapsId}.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@MappedSuperclass
@Getter
public abstract class AbstractAuditableAssignedUuidEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
