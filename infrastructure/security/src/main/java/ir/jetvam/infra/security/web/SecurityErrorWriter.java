package ir.jetvam.infra.security.web;

import ir.jetvam.infra.web.api.ApiError;
import ir.jetvam.infra.web.api.ApiResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Writes authentication and authorization failures using the common API envelope.
 * Security-filter failures therefore match errors produced by controller advice.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */
@RequiredArgsConstructor
public final class SecurityErrorWriter {

    private final ObjectMapper objectMapper;
    private final ApiResponseFactory responseFactory;

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        var error = new ApiError(code, message, List.of(), Map.of());
        objectMapper.writeValue(response.getOutputStream(), responseFactory.failure(error, request));
    }
}
