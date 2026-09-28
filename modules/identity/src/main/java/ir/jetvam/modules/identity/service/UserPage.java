package ir.jetvam.modules.identity.service;

import java.util.List;

/**
 * Returns an immutable page of safe user-account views.
 * Pagination metadata lets backoffice clients navigate without persistence types.
 *
 * @author reza jamshidi
 * @since 9/28/2026
 */
public record UserPage(
        List<UserView> items,
        long totalElements,
        int page,
        int size,
        int totalPages
) {

    public UserPage {
        items = List.copyOf(items);
    }
}
