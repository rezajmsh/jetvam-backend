package ir.jetvam.modules.inquiry.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * Stores runtime policy for one canonical inquiry capability.
 * Validity is database-managed so cached-result behavior can change without a release.
 *
 * @author reza jamshidi
 * @since 9/26/2026
 */
@Entity
@Table(name = "inquiry_definition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryDefinitionEntity extends AbstractAuditableUuidEntity {

    @Column(name = "inquiry_code", nullable = false, unique = true, length = 100)
    private String inquiryCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "validity_seconds", nullable = false)
    private long validitySeconds;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    public InquiryDefinitionEntity(String inquiryCode, String title, Duration validity, boolean enabled) {
        this.inquiryCode = Preconditions.requireText(inquiryCode, "inquiryCode").strip();
        this.title = Preconditions.requireText(title, "title").strip();
        update(validity, enabled);
    }

    public void update(Duration validity, boolean enabled) {
        Preconditions.requireNonNull(validity, "validity");
        Preconditions.require(!validity.isNegative(), "validity must not be negative");
        this.validitySeconds = validity.toSeconds();
        this.enabled = enabled;
    }

    public Duration validity() {
        return Duration.ofSeconds(validitySeconds);
    }
}
