package ir.jetvam.modules.settings.persistence;

import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.infra.persistence.entity.AbstractAuditableUuidEntity;
import ir.jetvam.modules.settings.model.SettingValueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Stores a runtime setting without leaking deployment configuration into business code.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
@Entity
@Table(name = "system_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SystemSettingEntity extends AbstractAuditableUuidEntity {

    @Column(name = "setting_key", nullable = false, unique = true, length = 150)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 2000)
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private SettingValueType valueType;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    public SystemSettingEntity(String key, String value, SettingValueType valueType, String description) {
        this.key = Preconditions.requireText(key, "key").strip();
        this.valueType = Preconditions.requireNonNull(valueType, "valueType");
        this.description = Preconditions.requireText(description, "description").strip();
        changeValue(value);
    }

    public void changeValue(String value) {
        String candidate = Preconditions.requireText(value, "value").strip();
        validate(candidate, valueType);
        this.value = candidate;
    }

    private static void validate(String value, SettingValueType type) {
        switch (type) {
            case BOOLEAN -> Preconditions.require(
                    "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value),
                    "boolean setting value must be true or false"
            );
            case INTEGER -> {
                try {
                    Long.parseLong(value);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("integer setting value is invalid", exception);
                }
            }
            case TEXT -> { }
        }
    }
}
