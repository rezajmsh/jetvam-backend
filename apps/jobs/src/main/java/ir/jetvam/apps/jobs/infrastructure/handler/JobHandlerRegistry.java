package ir.jetvam.apps.jobs.infrastructure.handler;

import ir.jetvam.common.exception.ConflictException;
import ir.jetvam.common.exception.ResourceNotFoundException;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves handlers by stable keys and rejects ambiguous registrations.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */
public final class JobHandlerRegistry {

    private final Map<String, JobHandler> handlers;

    public JobHandlerRegistry(Collection<JobHandler> handlers) {
        Map<String, JobHandler> registered = new LinkedHashMap<>();
        for (JobHandler handler : handlers) {
            String key = normalize(handler.key());
            JobHandler duplicate = registered.putIfAbsent(key, handler);
            if (duplicate != null) {
                throw new ConflictException("Duplicate job handler key: " + key);
            }
        }
        this.handlers = Map.copyOf(registered);
    }

    public JobHandler require(String key) {
        JobHandler handler = handlers.get(normalize(key));
        if (handler == null) {
            throw new ResourceNotFoundException("job handler", key);
        }
        return handler;
    }

    public List<String> keys() {
        return handlers.keySet().stream().sorted().toList();
    }

    private static String normalize(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Job handler key must not be blank");
        }
        return key.strip();
    }
}
