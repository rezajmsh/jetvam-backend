package ir.jetvam.modules.origination.model;

import ir.jetvam.infra.persistence.entity.AbstractUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents one centrally managed option used by application forms and queryable snapshots.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(name = "origination_application_profile_reference_option")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OriginationReferenceDataEntity extends AbstractUuidEntity {
    @Column(name = "category", nullable = false, length = 100)
    private String category;
    @Column(name = "code", nullable = false, length = 100)
    private String code;
    @Column(name = "label", nullable = false, length = 200)
    private String label;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    @Column(name = "active", nullable = false)
    private boolean active;
}
