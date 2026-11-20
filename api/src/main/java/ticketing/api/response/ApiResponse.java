package ticketing.api.response;

/**
 * 모든 API의 응답 형식.
 * 인증 서버(second_festival_backend)와 같은 형식이라 프론트 apiFetch가 그대로 해석한다.
 */
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        ErrorResponse error
) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, "SUCCESS", message, data, null);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, "SUCCESS", message, null, null);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null, ErrorResponse.of(code, message));
    }

    public static ApiResponse<Void> fail(String code, String message, ErrorResponse errorResponse) {
        return new ApiResponse<>(false, code, message, null, errorResponse);
    }
}
