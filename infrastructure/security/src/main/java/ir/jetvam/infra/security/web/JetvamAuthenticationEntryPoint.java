package ir.jetvam.infra.security.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Converts missing or invalid bearer-token failures to the shared API format.
 * It is invoked before requests reach MVC controllers.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@RequiredArgsConstructor
public final class JetvamAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorWriter errorWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        errorWriter.write(request, response, 401, "SECURITY.UNAUTHORIZED", "Authentication is required");
    }
}
