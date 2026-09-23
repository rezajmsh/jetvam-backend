package ir.jetvam.infra.i18n.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Maps a localized database message for Spring Data queries.
 * Message mutation is intentionally kept outside the runtime resolver.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@Entity
@Immutable
@Table(name = "i18n_message")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class I18nMessageEntity {

    @Id
    private Long id;

    @Version
    private long version;

    @Column(name = "message_key", nullable = false, length = 255)
    private String messageKey;

    @Column(name = "locale", nullable = false, length = 35)
    private String locale;

    @Column(name = "message_text", nullable = false)
    private String messageText;

    @Column(name = "active", nullable = false)
    private boolean active;
}
