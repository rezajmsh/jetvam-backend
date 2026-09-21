package ir.jetvam.infra.web.api;

import ir.jetvam.common.time.TimeProvider;
import ir.jetvam.infra.observability.trace.TraceContextProvider;
import ir.jetvam.infra.web.request.WebRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ApiResponseFactory {

    private final TimeProvider timeProvider;
    private final TraceContextProvider traceContextProvider;

    public <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        return ApiResponse.success(data, metadata(request));
    }

    public <T> ApiResponse<T> failure(ApiError error, HttpServletRequest request) {
        return ApiResponse.failure(error, metadata(request));
    }

    private ApiMeta metadata(HttpServletRequest request) {
        Object requestId = request.getAttribute(WebRequestAttributes.REQUEST_ID);
        var traceContext = traceContextProvider.current();
        return new ApiMeta(
                timeProvider.now(),
                requestId == null ? null : requestId.toString(),
                traceContext.present() ? traceContext.traceId() : null
        );
    }
}
