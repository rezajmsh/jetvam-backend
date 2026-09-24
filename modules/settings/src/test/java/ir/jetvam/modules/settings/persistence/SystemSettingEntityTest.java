package ir.jetvam.modules.settings.persistence;

import ir.jetvam.modules.settings.model.SettingValueType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** Protects the persisted setting type contract from invalid administrative values. */
class SystemSettingEntityTest {

    @Test
    void validatesBooleanUpdates() {
        SystemSettingEntity setting = new SystemSettingEntity(
                "security.test.two-factor-required",
                "false",
                SettingValueType.BOOLEAN,
                "Test policy"
        );

        setting.changeValue("true");

        assertThat(setting.getValue()).isEqualTo("true");
        assertThatIllegalArgumentException().isThrownBy(() -> setting.changeValue("enabled"));
    }
}
