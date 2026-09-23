package ir.jetvam.infra.web.api;

/**
 * Defines the common success and failure envelope returned by Jetvam APIs.
 * Data, error and correlation metadata use one stable contract.
 *
 * @author reza jamshidi
 * @since 9/21/2026
 */

public record ApiResponse<T>(boolean success, T data, ApiError error, ApiMeta meta) {

    public static <T> ApiResponse<T> success(T data, ApiMeta meta) {
        return new ApiResponse<>(true, data, null, meta);
    }

    public static <T> ApiResponse<T> failure(ApiError error, ApiMeta meta) {
        return new ApiResponse<>(false, null, error, meta);
    }
}
