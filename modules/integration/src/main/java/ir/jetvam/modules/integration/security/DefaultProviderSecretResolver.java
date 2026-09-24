package ir.jetvam.modules.integration.security;

import ir.jetvam.common.validation.Preconditions;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves environment and file-backed secrets while rejecting inline database values.
 * File timestamps provide automatic client refresh when operators rotate credentials.
 *
 * @author reza jamshidi
 * @since 9/24/2026
 */
@Component
public class DefaultProviderSecretResolver implements ProviderSecretResolver {

    private static final String ENV_PREFIX = "env:";
    private static final String FILE_PREFIX = "file:";

    private final Environment environment;

    public DefaultProviderSecretResolver(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String resolve(String reference) {
        String normalized = Preconditions.requireText(reference, "secretReference").strip();
        if (normalized.startsWith(ENV_PREFIX)) {
            String variable = normalized.substring(ENV_PREFIX.length());
            return Preconditions.requireText(environment.getProperty(variable), "environment secret " + variable);
        }
        if (normalized.startsWith(FILE_PREFIX)) {
            try {
                return Files.readString(path(normalized), StandardCharsets.UTF_8).strip();
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read provider secret file", exception);
            }
        }
        throw new IllegalArgumentException("Unsupported secret reference scheme");
    }

    @Override
    public String fingerprint(String reference) {
        String normalized = Preconditions.requireText(reference, "secretReference").strip();
        if (normalized.startsWith(ENV_PREFIX)) {
            return normalized;
        }
        if (normalized.startsWith(FILE_PREFIX)) {
            try {
                Path path = path(normalized);
                return normalized + ":" + Files.getLastModifiedTime(path).toMillis() + ":" + Files.size(path);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not inspect provider secret file", exception);
            }
        }
        throw new IllegalArgumentException("Unsupported secret reference scheme");
    }

    private static Path path(String reference) {
        return Paths.get(reference.substring(FILE_PREFIX.length())).toAbsolutePath().normalize();
    }
}
