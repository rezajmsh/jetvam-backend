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
    public static final String ROUTE = WebRequestAttributes.class.getName() + ".route";
    public static final String ERROR_TYPE = WebRequestAttributes.class.getName() + ".errorType";
    public static final String ERROR_MESSAGE = WebRequestAttributes.class.getName() + ".errorMessage";
    public static final String OAUTH2_ERROR_CODE = WebRequestAttributes.class.getName() + ".oauth2ErrorCode";
    public static final String OAUTH2_GRANT_TYPE = WebRequestAttributes.class.getName() + ".oauth2GrantType";
    public static final String OAUTH2_CLIENT_ID = WebRequestAttributes.class.getName() + ".oauth2ClientId";
    public static final String OAUTH2_CLIENT_AUTHENTICATION_METHOD =
            WebRequestAttributes.class.getName() + ".oauth2ClientAuthenticationMethod";

    private WebRequestAttributes() {
    }
}
