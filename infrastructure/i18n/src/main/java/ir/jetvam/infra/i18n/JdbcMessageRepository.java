package ir.jetvam.infra.i18n;

import ir.jetvam.common.validation.Preconditions;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

public final class JdbcMessageRepository implements MessageRepository {

    private final JdbcTemplate jdbcTemplate;
    private final String selectByLocaleSql;

    public JdbcMessageRepository(DataSource dataSource, String tableName) {
        this.jdbcTemplate = new JdbcTemplate(Preconditions.requireNonNull(dataSource, "dataSource"));
        String safeTableName = validateTableName(tableName);
        this.selectByLocaleSql = "select message_key, message_text from " + safeTableName
                + " where active = true and lower(locale) = lower(?) order by message_key";
    }

    @Override
    public Map<String, String> findActiveMessages(String localeTag) {
        Map<String, String> messages = new LinkedHashMap<>();
        jdbcTemplate.query(
                selectByLocaleSql,
                (resultSet, rowNumber) -> Map.entry(resultSet.getString(1), resultSet.getString(2)),
                localeTag
        ).forEach(entry -> messages.put(entry.getKey(), entry.getValue()));
        return Map.copyOf(messages);
    }

    private static String validateTableName(String tableName) {
        String value = Preconditions.requireText(tableName, "jetvam.i18n.table-name");
        Preconditions.require(
                value.matches("[a-zA-Z][a-zA-Z0-9_]*"),
                "jetvam.i18n.table-name must be a simple SQL identifier"
        );
        return value;
    }
}
