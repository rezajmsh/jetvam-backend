package ir.jetvam.infra.i18n.persistence;

import ir.jetvam.infra.i18n.MessageRepository;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapts the Spring Data message repository to the i18n lookup port.
 * The resolver remains independent from JPA entities and query mechanics.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@RequiredArgsConstructor
public final class JpaMessageRepository implements MessageRepository {

    private final I18nMessageJpaRepository repository;

    @Override
    public Map<String, String> findActiveMessages(String localeTag) {
        Map<String, String> messages = new LinkedHashMap<>();
        repository.findAllByActiveTrueAndLocaleIgnoreCaseOrderByMessageKey(localeTag)
                .forEach(message -> messages.put(message.getMessageKey(), message.getMessageText()));
        return Map.copyOf(messages);
    }
}
