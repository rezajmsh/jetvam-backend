package ir.jetvam.common.exception;

import java.util.Map;

/**
 * Represents a resource not found failure in the common exception hierarchy.
 * Its stable error code can be mapped consistently at API boundaries.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public final class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(
                CommonErrorCode.RESOURCE_NOT_FOUND,
                "%s was not found".formatted(resourceType),
                Map.of("resourceType", resourceType, "identifier", String.valueOf(identifier))
        );
    }
}
