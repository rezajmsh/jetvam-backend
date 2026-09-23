package ir.jetvam.infra.i18n.persistence;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies mapping from localized JPA records to the resolver message contract.
 * Query ordering and duplicate behavior stay encapsulated in the persistence adapter.
 *
 * @author reza jamshidi
 * @since 9/22/2026
 */
class JpaMessageRepositoryTest {

    @Test
    void returnsActiveMessagesAsImmutableMap() {
        I18nMessageJpaRepository repository = mock(I18nMessageJpaRepository.class);
        I18nMessageEntity first = message("validation.required", "مقدار الزامی است");
        I18nMessageEntity second = message("validation.invalid", "مقدار نامعتبر است");
        when(repository.findAllByActiveTrueAndLocaleIgnoreCaseOrderByMessageKey("fa-IR"))
                .thenReturn(List.of(first, second));

        var messages = new JpaMessageRepository(repository).findActiveMessages("fa-IR");

        assertThat(messages).containsEntry("validation.required", "مقدار الزامی است")
                .containsEntry("validation.invalid", "مقدار نامعتبر است");
        assertThat(messages).isUnmodifiable();
    }

    private static I18nMessageEntity message(String key, String text) {
        I18nMessageEntity message = mock(I18nMessageEntity.class);
        when(message.getMessageKey()).thenReturn(key);
        when(message.getMessageText()).thenReturn(text);
        return message;
    }
}
