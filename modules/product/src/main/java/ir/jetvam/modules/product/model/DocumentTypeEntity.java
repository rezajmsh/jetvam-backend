package ir.jetvam.modules.product.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Defines one centrally managed document type and its upload constraints.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(name = "product_document_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentTypeEntity extends AbstractAuditableUuidEntity {

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    @Column(name = "allowed_content_types", nullable = false, length = 1000)
    private String allowedContentTypes;
    @Column(name = "maximum_size_bytes", nullable = false)
    private long maximumSizeBytes;
    @Column(name = "active", nullable = false)
    private boolean active;

    public DocumentTypeEntity(
            String code, String title, String allowedContentTypes, long maximumSizeBytes, boolean active
    ) {
        this.code = normalize(code, "code");
        update(title, allowedContentTypes, maximumSizeBytes, active);
    }

    public void update(String title, String allowedContentTypes, long maximumSizeBytes, boolean active) {
        this.title = Preconditions.requireText(title, "title").strip();
        this.allowedContentTypes = Preconditions.requireText(allowedContentTypes, "allowedContentTypes").strip();
        this.maximumSizeBytes = Preconditions.requirePositive(maximumSizeBytes, "maximumSizeBytes");
        this.active = active;
    }

    private static String normalize(String value, String field) {
        return Preconditions.requireText(value, field).strip().toUpperCase();
    }
}
