package ir.jetvam.apps.jobs.infrastructure.handler;

import ir.jetvam.common.exception.ConflictException;
import ir.jetvam.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobHandlerRegistryTest {

    @Test
    void resolvesHandlersByStableKey() {
        JobHandler handler = handler("notification-dispatch");
        JobHandlerRegistry registry = new JobHandlerRegistry(List.of(handler));

        assertSame(handler, registry.require("notification-dispatch"));
        assertEquals(List.of("notification-dispatch"), registry.keys());
    }

    @Test
    void rejectsDuplicateKeys() {
        assertThrows(ConflictException.class, () -> new JobHandlerRegistry(List.of(
                handler("same-key"), handler("same-key")
        )));
    }

    @Test
    void rejectsUnknownKeys() {
        JobHandlerRegistry registry = new JobHandlerRegistry(List.of());
        assertThrows(ResourceNotFoundException.class, () -> registry.require("missing"));
    }

    private static JobHandler handler(String key) {
        return new JobHandler() {
            @Override
            public String key() {
                return key;
            }

            @Override
            public JobResult execute(JobContext context) {
                return JobResult.completed();
            }
        };
    }
}
