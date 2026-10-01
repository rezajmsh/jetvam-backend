package ir.jetvam.modules.inquiry.execution;

import java.util.Map;

/**
 * Durable opaque state passed back only to the typed provider that owns an inquiry protocol.
 * The generic execution workflow persists this context without inferring protocol stages from it.
 *
 * @author reza jamshidi
 * @since 10/1/2026
 */
public record InquiryExecutionContext(
        String providerCode,
        Map<String, String> data
) {

    public InquiryExecutionContext {
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    public static InquiryExecutionContext empty() {
        return new InquiryExecutionContext(null, Map.of());
    }

    public String value(String key) {
        return data.get(key);
    }
}
