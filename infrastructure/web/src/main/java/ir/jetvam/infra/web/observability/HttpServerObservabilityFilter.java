package ir.jetvam.infra.web.observability;

import io.micrometer.core.instrument.Tag;
import ir.jetvam.infra.observability.config.JetvamObservabilityProperties;
import ir.jetvam.infra.observability.logging.OtelEventLogger;
import ir.jetvam.infra.observability.logging.OtelEventType;
import ir.jetvam.infra.observability.metrics.InfrastructureMetrics;
import ir.jetvam.infra.web.request.WebRequestAttributes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Processes servlet traffic for http server observability concerns.
 * It applies shared behavior before requests reach application controllers.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

@RequiredArgsConstructor
public class HttpServerObservabilityFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger("jetvam.http.server");
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,100}");

    private final JetvamObservabilityProperties properties;
    private final OtelEventLogger eventLogger;
    private final InfrastructureMetrics metrics;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var settings = properties.getHttp().getServer();
        String requestId = requestId(request, settings.getRequestIdHeader());
        request.setAttribute(WebRequestAttributes.REQUEST_ID, requestId);
        response.setHeader(settings.getRequestIdHeader(), requestId);

        if (!settings.isEnabled() || isExcluded(request.getRequestURI(), settings.getExcludedPaths())) {
            filterChain.doFilter(request, response);
            return;
        }

        long startedAt = System.nanoTime();
        try (MDC.MDCCloseable ignored = MDC.putCloseable("request.id", requestId)) {
            Throwable failure = null;
            try {
                logRequest(request);
                filterChain.doFilter(request, response);
            } catch (IOException | ServletException | RuntimeException exception) {
                failure = exception;
                throw exception;
            } finally {
                Duration duration = Duration.ofNanos(System.nanoTime() - startedAt);
                logResponse(request, response, duration, failure);
                recordMetric(request, response, duration);
            }
        }
    }

    private void logRequest(HttpServletRequest request) {
        var settings = properties.getHttp().getServer();
        if (!properties.getLogging().isEnabled() || !settings.isLogRequest()) {
            return;
        }
        Map<String, Object> attributes = baseAttributes(request);
        attributes.put("event.phase", "start");
        attributes.put("span.kind", "server");
        attributes.put("http.request.body.size", Math.max(request.getContentLengthLong(), 0));
        for (String header : settings.getRequestHeaderAllowList()) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                attributes.put("http.request.header." + normalize(header), value);
            }
        }
        eventLogger.log(
                LOGGER,
                properties.getLogging().getLevel(),
                "http.server.request.started",
                OtelEventType.HTTP_SERVER_REQUEST,
                "HTTP request started",
                attributes
        );
    }

    private void logResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            Duration duration,
            Throwable failure
    ) {
        var settings = properties.getHttp().getServer();
        if (!properties.getLogging().isEnabled() || !settings.isLogResponse()) {
            return;
        }
        Map<String, Object> attributes = baseAttributes(request);
        attributes.put("event.phase", "end");
        attributes.put("span.kind", "server");
        attributes.put("http.route", route(request));
        attributes.put("http.response.status_code", response.getStatus());
        contentLength(response).ifPresent(length -> attributes.put("http.response.body.size", length));
        attributes.put("duration_ms", duration.toNanos() / 1_000_000.0);
        attributes.put("operation.outcome", outcome(response.getStatus(), failure));
        copyRequestAttribute(attributes, request, WebRequestAttributes.OAUTH2_ERROR_CODE, "oauth2.error.code");
        copyRequestAttribute(attributes, request, WebRequestAttributes.OAUTH2_GRANT_TYPE, "oauth2.grant.type");
        copyRequestAttribute(attributes, request, WebRequestAttributes.OAUTH2_CLIENT_ID, "oauth2.client.id");
        copyRequestAttribute(
                attributes,
                request,
                WebRequestAttributes.OAUTH2_CLIENT_AUTHENTICATION_METHOD,
                "oauth2.client.authentication_method"
        );
        copyRequestAttribute(attributes, request, WebRequestAttributes.ERROR_TYPE, "error.type");
        copyRequestAttribute(attributes, request, WebRequestAttributes.ERROR_MESSAGE, "error.message");
        if (failure != null) {
            attributes.put("error.type", failure.getClass().getName());
        }
        eventLogger.log(
                LOGGER,
                responseLogLevel(request, response, failure),
                "http.server.request.completed",
                OtelEventType.HTTP_SERVER_REQUEST,
                "HTTP request completed",
                attributes,
                failure
        );
    }

    private void recordMetric(HttpServletRequest request, HttpServletResponse response, Duration duration) {
        var settings = properties.getHttp().getServer();
        if (!properties.getMetrics().isEnabled() || !settings.isMetricsEnabled()) {
            return;
        }
        metrics.record("http.server.request.duration", duration, List.of(
                Tag.of("method", request.getMethod()),
                Tag.of("route", route(request)),
                Tag.of("status", String.valueOf(response.getStatus())),
                Tag.of("outcome", outcome(response.getStatus(), null))
        ));
    }

    private Map<String, Object> baseAttributes(HttpServletRequest request) {
        var settings = properties.getHttp().getServer();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("http.request.method", request.getMethod());
        attributes.put("url.scheme", request.getScheme());
        attributes.put("url.path", request.getRequestURI());
        if (settings.isIncludeQueryString() && request.getQueryString() != null) {
            attributes.put("url.query", request.getQueryString());
        }
        attributes.put("server.address", request.getServerName());
        attributes.put("server.port", request.getServerPort());
        attributes.put("client.address", clientAddress(request));
        attributes.put("network.protocol.version", request.getProtocol());
        return attributes;
    }

    private String clientAddress(HttpServletRequest request) {
        String header = properties.getHttp().getServer().getClientIpHeader();
        if (header != null && !header.isBlank()) {
            String forwarded = request.getHeader(header);
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",", 2)[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String requestId(HttpServletRequest request, String header) {
        String candidate = request.getHeader(header);
        return candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()
                ? candidate
                : UUID.randomUUID().toString();
    }

    private static String route(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern != null) {
            return pattern.toString();
        }
        Object diagnosticRoute = request.getAttribute(WebRequestAttributes.ROUTE);
        return diagnosticRoute == null ? "UNKNOWN" : diagnosticRoute.toString();
    }

    private org.slf4j.event.Level responseLogLevel(
            HttpServletRequest request,
            HttpServletResponse response,
            Throwable failure
    ) {
        if (failure != null || response.getStatus() >= 500) {
            return org.slf4j.event.Level.ERROR;
        }
        if (request.getAttribute(WebRequestAttributes.OAUTH2_ERROR_CODE) != null) {
            return org.slf4j.event.Level.WARN;
        }
        return properties.getLogging().getLevel();
    }

    private static void copyRequestAttribute(
            Map<String, Object> attributes,
            HttpServletRequest request,
            String requestAttribute,
            String logAttribute
    ) {
        Object value = request.getAttribute(requestAttribute);
        if (value != null) {
            attributes.put(logAttribute, value);
        }
    }

    private static String outcome(int status, Throwable failure) {
        if (failure != null || status >= 500) {
            return "server_error";
        }
        if (status >= 400) {
            return "client_error";
        }
        return "success";
    }

    private static boolean isExcluded(String path, List<String> excludedPaths) {
        return excludedPaths.stream().anyMatch(path::startsWith);
    }

    private static String normalize(String header) {
        return header.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static java.util.OptionalLong contentLength(HttpServletResponse response) {
        String value = response.getHeader("Content-Length");
        if (value == null) {
            return java.util.OptionalLong.empty();
        }
        try {
            return java.util.OptionalLong.of(Long.parseLong(value));
        } catch (NumberFormatException ignored) {
            return java.util.OptionalLong.empty();
        }
    }
}
