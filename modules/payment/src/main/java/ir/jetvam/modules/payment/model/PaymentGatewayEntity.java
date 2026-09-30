package ir.jetvam.modules.payment.model;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stores configurable IPG routing data while keeping provider secrets outside application code.
 *
 * @author reza jamshidi
 * @since 9/29/2026
 */
@Entity
@Table(name = "payment_gateway")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentGatewayEntity extends AbstractAuditableUuidEntity {
    @Column(name = "gateway_code", nullable = false, unique = true, length = 100)
    private String gatewayCode;
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    @Column(name = "adapter_code", nullable = false, length = 100)
    private String adapterCode;
    @Column(name = "configuration_json", nullable = false, columnDefinition = "text")
    private String configurationJson;
    @Column(name = "active", nullable = false)
    private boolean active;

    public void update(String title, String configurationJson, boolean active) {
        this.title = Preconditions.requireText(title, "title").strip();
        this.configurationJson = configurationJson == null || configurationJson.isBlank() ? "{}" : configurationJson.strip();
        this.active = active;
    }
}
