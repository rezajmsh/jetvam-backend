package ir.jetvam.infra.web.request;

/**
 * Defines servlet request-attribute names shared by web infrastructure.
 * Central constants prevent filters and error handlers from drifting.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public final class WebRequestAttributes {

    public static final String REQUEST_ID = WebRequestAttributes.class.getName() + ".requestId";

    private WebRequestAttributes() {
    }
}
