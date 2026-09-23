package ir.jetvam.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Adds automatic creation and modification timestamps to UUID entities.
 * Hibernate maintains audit fields consistently for persisted records.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@MappedSuperclass
@Getter
public abstract class AbstractAuditableUuidEntity extends AbstractUuidEntity {

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}
