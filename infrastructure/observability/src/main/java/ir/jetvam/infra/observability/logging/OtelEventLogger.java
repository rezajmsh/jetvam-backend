package ir.jetvam.infra.observability.logging;

import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.util.LinkedHashMap;
import java.util.Map;

/** Emits SLF4J key/value events that are preserved by the OTel JSON formatter.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public class OtelEventLogger {

    public void log(
            Logger logger,
            Level level,
            String eventName,
            OtelEventType eventType,
            String message,
            Map<String, ?> attributes
    ) {
        log(logger, level, eventName, eventType, message, attributes, null);
    }

    public void log(
            Logger logger,
            Level level,
            String eventName,
            OtelEventType eventType,
            String message,
            Map<String, ?> attributes,
            Throwable cause
    ) {
        var builder = logger.atLevel(level)
                .addKeyValue("event.name", eventName)
                .addKeyValue("event.type", eventType.value());
        if (attributes != null) {
            attributes.forEach((key, value) -> {
                if (key != null && value != null) {
                    builder.addKeyValue(key, value);
                }
            });
        }
        if (cause != null) {
            builder.setCause(cause);
        }
        builder.log(message);
    }

    public Map<String, Object> attributes(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues must contain an even number of elements");
        }
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (int index = 0; index < keyValues.length; index += 2) {
            attributes.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return attributes;
    }
}
