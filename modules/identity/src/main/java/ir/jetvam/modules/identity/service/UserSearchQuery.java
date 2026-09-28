package ir.jetvam.modules.identity.service;

import ir.jetvam.common.security.UserCategory;
import ir.jetvam.common.validation.Preconditions;
import ir.jetvam.modules.identity.model.UserAccountStatus;

/**
 * Defines bounded filters and pagination for administrative user searches.
 * Search text may match a username, display name or authentication mobile.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
public record UserSearchQuery(
        String text,
        UserAccountStatus status,
        UserCategory category,
        int page,
        int size
) {

    public UserSearchQuery {
        Preconditions.requireNonNegative(page, "page");
        Preconditions.requireInRange(size, 1, 100, "size");
        text = text == null || text.isBlank() ? null : text.strip();
    }
}
