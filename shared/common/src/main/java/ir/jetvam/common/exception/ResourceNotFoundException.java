package ir.jetvam.common.exception;

import java.util.Map;

public final class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceType, Object identifier) {
        super(
                CommonErrorCode.RESOURCE_NOT_FOUND,
                "%s was not found".formatted(resourceType),
                Map.of("resourceType", resourceType, "identifier", String.valueOf(identifier))
        );
    }
}
