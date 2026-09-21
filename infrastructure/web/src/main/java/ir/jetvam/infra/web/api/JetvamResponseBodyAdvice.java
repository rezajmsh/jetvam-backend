package ir.jetvam.infra.web.api;

import ir.jetvam.infra.web.config.JetvamWebProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
public class JetvamResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final ApiResponseFactory responseFactory;
    private final JetvamWebProperties properties;

    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        Class<?> type = returnType.getParameterType();
        return properties.getResponse().isEnvelopeEnabled()
                && !ApiResponse.class.isAssignableFrom(type)
                && !ProblemDetail.class.isAssignableFrom(type)
                && !Resource.class.isAssignableFrom(type)
                && !byte[].class.equals(type)
                && !CharSequence.class.isAssignableFrom(type);
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (body instanceof ApiResponse<?> || body instanceof ProblemDetail || isExcluded(request.getURI().getPath())) {
            return body;
        }
        if (response instanceof org.springframework.http.server.ServletServerHttpResponse servletResponse
                && !HttpStatusCode.valueOf(servletResponse.getServletResponse().getStatus()).is2xxSuccessful()) {
            return body;
        }
        HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
        return responseFactory.success(body, servletRequest);
    }

    private boolean isExcluded(String path) {
        return properties.getResponse().getExcludedPaths().stream().anyMatch(path::startsWith);
    }
}
