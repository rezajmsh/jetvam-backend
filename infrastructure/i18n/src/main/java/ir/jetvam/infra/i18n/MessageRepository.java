package ir.jetvam.infra.i18n;

import java.util.Map;

/**
 * Defines persistence access for localized messages stored in the database.
 * Resolvers remain independent from the concrete JDBC implementation.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public interface MessageRepository {

    Map<String, String> findActiveMessages(String localeTag);
}
