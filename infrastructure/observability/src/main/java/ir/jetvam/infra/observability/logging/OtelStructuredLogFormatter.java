package ir.jetvam.infra.observability.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import org.slf4j.event.KeyValuePair;
import org.springframework.boot.logging.structured.StructuredLogFormatter;
import org.springframework.core.env.Environment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON formatter aligned with the OpenTelemetry log data model.
 *
 * <p>Business and infrastructure fields are kept under {@code attributes};
 * trace and span identifiers are promoted to top-level correlation fields.</p>
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
public final class OtelStructuredLogFormatter implements StructuredLogFormatter<ILoggingEvent> {

    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID = "spanId";
    private final boolean includeStackTrace;
    private final String defaultServiceName;
    private final String defaultServiceVersion;

    public OtelStructuredLogFormatter(Environment environment) {
        this.includeStackTrace = environment.getProperty(
                "jetvam.observability.logging.include-stack-trace",
                Boolean.class,
                true
        );
        this.defaultServiceName = environment.getProperty("spring.application.name", "jetvam");
        this.defaultServiceVersion = environment.getProperty("spring.application.version", "unknown");
    }

    @Override
    public String format(ILoggingEvent event) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("logger.name", event.getLoggerName());
        attributes.put("thread.name", event.getThreadName());
        event.getMDCPropertyMap().forEach((key, value) -> {
            if (!TRACE_ID.equals(key) && !SPAN_ID.equals(key)) {
                attributes.put(key, value);
            }
        });
        if (event.getKeyValuePairs() != null) {
            for (KeyValuePair pair : event.getKeyValuePairs()) {
                attributes.put(pair.key, pair.value);
            }
        }
        if (event.getThrowableProxy() != null) {
            attributes.put("exception.type", event.getThrowableProxy().getClassName());
            attributes.put("exception.message", event.getThrowableProxy().getMessage());
            if (includeStackTrace) {
                attributes.put("exception.stacktrace", ThrowableProxyUtil.asString(event.getThrowableProxy()));
            }
        }

        Map<String, String> mdc = event.getMDCPropertyMap();
        Map<String, String> context = event.getLoggerContextVO() == null
                ? Map.of()
                : event.getLoggerContextVO().getPropertyMap();
        String serviceName = context.getOrDefault("service.name", defaultServiceName);
        String serviceVersion = context.getOrDefault("service.version", defaultServiceVersion);

        StringBuilder json = new StringBuilder(512);
        json.append('{');
        member(json, "timestamp", event.getInstant().toString(), false);
        member(json, "observed_timestamp", event.getInstant().toString());
        numberMember(json, "severity_number", severityNumber(event));
        member(json, "severity_text", event.getLevel().levelStr);
        member(json, "body", event.getFormattedMessage());
        optionalMember(json, "trace_id", mdc.get(TRACE_ID));
        optionalMember(json, "span_id", mdc.get(SPAN_ID));
        json.append(",\"resource\":{");
        member(json, "service.name", serviceName, false);
        member(json, "service.version", serviceVersion);
        json.append('}');
        json.append(",\"attributes\":");
        object(json, attributes);
        json.append('}').append(System.lineSeparator());
        return json.toString();
    }

    private static int severityNumber(ILoggingEvent event) {
        return switch (event.getLevel().levelInt) {
            case 5000 -> 1;
            case 10000 -> 5;
            case 20000 -> 9;
            case 30000 -> 13;
            case 40000 -> 17;
            default -> 0;
        };
    }

    private static void object(StringBuilder json, Map<String, ?> values) {
        json.append('{');
        boolean first = true;
        for (var entry : values.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (!first) {
                json.append(',');
            }
            string(json, entry.getKey());
            json.append(':');
            value(json, entry.getValue());
            first = false;
        }
        json.append('}');
    }

    private static void value(StringBuilder json, Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else {
            string(json, String.valueOf(value));
        }
    }

    private static void member(StringBuilder json, String name, String value) {
        member(json, name, value, true);
    }

    private static void member(StringBuilder json, String name, String value, boolean comma) {
        if (comma) {
            json.append(',');
        }
        string(json, name);
        json.append(':');
        string(json, value);
    }

    private static void optionalMember(StringBuilder json, String name, String value) {
        if (value != null && !value.isBlank()) {
            member(json, name, value);
        }
    }

    private static void numberMember(StringBuilder json, String name, int value) {
        json.append(',');
        string(json, name);
        json.append(':').append(value);
    }

    private static void string(StringBuilder json, String value) {
        json.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        json.append('"');
    }
}
