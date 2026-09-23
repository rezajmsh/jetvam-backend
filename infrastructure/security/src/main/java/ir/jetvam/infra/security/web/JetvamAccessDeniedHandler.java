package ir.jetvam.infra.security.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/**
 * Converts authorization failures to the shared API error envelope.
 * The handler keeps HTTP 403 responses consistent with controller failures.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@RequiredArgsConstructor
public final class JetvamAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorWriter errorWriter;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException, ServletException {
        errorWriter.write(request, response, 403, "SECURITY.ACCESS_DENIED", "Access is denied");
    }
}
